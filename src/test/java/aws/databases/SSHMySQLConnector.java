package aws.databases;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Parameters;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SSHMySQLConnector extends AbstractTest{
    private static final String DB_URL = "jdbc:mysql://localhost:3306/cloudximages"; // Change
    private static final int LOCAL_PORT = 3306; // Local port for SSH tunnel
    private static final int REMOTE_PORT = 3306; // MySQL port on the remote server
    private static final Logger log = LoggerFactory.getLogger(SSHMySQLConnector.class);
    private ServerSocket serverSocket;
    private LocalPortForwarder forwarder;
    //private Connection conn;
    Connection conn = null;

    private Connection getConnection() throws SQLException {

        Properties connectionProps = new Properties();
        connectionProps.put("user", DB_USER_NAME);
        connectionProps.put("password", DB_USER_PASSWORD);

        return DriverManager.getConnection(DB_URL, connectionProps);
    }

    protected List<ImageMetadata> getImageMetadataFromRds(String imageName) throws IOException, SQLException {
        SSHClient ssh = new SSHClient();
        List<ImageMetadata> metadataList = new ArrayList<>();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        try {
            ssh.connect(PUBLIC_IP);
            KeyProvider keys = ssh.loadKeys(PATH_TO_PEM_FILE);
            ssh.authPublickey("ec2-user", keys);


                // Set up port forwarding from local port to remote MySQL port
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress("localhost", LOCAL_PORT));
                Parameters params = new Parameters("localhost", LOCAL_PORT, DB_INSTANCE_IDENTIFIER, REMOTE_PORT);
                forwarder = new LocalPortForwarder(ssh.getConnection(), params, serverSocket,  ssh.getTransport().getConfig().getLoggerFactory());

                // Start the port forwarding in a separate thread
                Thread forwarderThread = new Thread(() -> {
                    try {
                        forwarder.listen();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                forwarderThread.start();
            // Now you can connect to the MySQL database using the local port
            conn = getConnection();
                String query = "SELECT * FROM images i WHERE i.object_key = 'images/0c7da500-f9c7-4e92-a105-d4f9529cdcdf-ES_v1_configuration.jpg'";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                   // stmt.setString(1, imageName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            metadataList.add( new ImageMetadata(
                                    rs.getString("id"),
                                    rs.getString("object_key"),
                                    rs.getLong("object_size"),
                                    rs.getString("object_type"),
                                    rs.getTimestamp("last_modified")));

                        }
                    }
                }

             catch (SQLException e) {
                log.error("SQL Exception: ", e);
                e.printStackTrace();
            }
        } finally {
            // Close database connection
            if (conn != null && !conn.isClosed()) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error closing connection: ", e);
                }
            }
            // Close port forwarder
            if (forwarder != null) {
                try {
                    forwarder.close();
                } catch (IOException e) {
                    log.error("Error closing forwarder: ", e);
                }
            }
            // Close server socket
            if (serverSocket != null ) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    log.error("Error closing server socket: ", e);
                }
            }
            ssh.disconnect();
        }
        return metadataList;
    }
    @Test
    public void rdsConnectionTest() {
        SSHMySQLConnector connector = new SSHMySQLConnector();
        try {
            List<ImageMetadata> metadataList = connector.getImageMetadataFromRds("ES_v1_configuration.jpg");
            if (metadataList.isEmpty()) {
                System.out.println(metadataList);
            }for (ImageMetadata metadata : metadataList) {
                System.out.println("Image Metadata: " + metadata);
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
