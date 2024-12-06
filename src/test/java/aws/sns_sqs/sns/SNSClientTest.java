package aws.sns_sqs.sns;

import aws.sns_sqs.SNSHelper;
import org.junit.Test;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
public class SNSClientTest {
    SNSHelper snsHelper;
    EmailHelper emailHelper;
    String topicArn = "arn:aws:sns:Topic";
    String emailAddress = "myemail@email.com";
    SNSClient snsClient = new SNSClient(topicArn, "email", "eu-central-1");
    @Test
    public void testSubscription() {

        SubscribeResponse subscribeResponse = snsClient.subscribe(emailAddress);
        assertNotNull(subscribeResponse.subscriptionArn());

        System.out.println(snsClient.listSubscriptions().stream().collect(Collectors.toList()));
                //.toList());

    }
    @Test
    public void testUnsubscribe() {

        snsClient.unsubscribe(emailAddress);
    }
}
