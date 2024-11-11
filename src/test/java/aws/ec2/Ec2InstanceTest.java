package aws.ec2;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.Optional;

public class Ec2InstanceTest extends AbstractTest {

    @Test
    public void testEc2InstancesConfiguration() {
        DescribeInstancesResponse response = ec2Client.describeInstances();
        if(response.reservations().size() == 2) {
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    validateInstance(instance);
                }
            }
        } else Assert.assertTrue("No any instances for this user are present", false);
    }

    private void validateInstance(Instance instance) {
        validateInstanceType(instance);
        validateTags(instance);
        validateRootBlockDevice(instance);
        validateInstanceOS(instance);
        validatePublicIp(instance);
    }

    private void validateInstanceType(Instance instance) {
        logger.info(instance.instanceType() +"\n");
        Assert.assertEquals("Instance type mismatch", InstanceType.T2_MICRO, instance.instanceType());
    }

    private void validateTags(Instance instance) {
        List<Tag> tags = instance.tags();
        logger.info(tags.toString());
                boolean hasCloudxTag = tags.stream().anyMatch(tag -> "cloudx".equals(tag.key()) && "qa".equals(tag.value()));
                Assert.assertTrue("Instance does not have the correct tags", hasCloudxTag);
    }

    private void validateRootBlockDevice(Instance instance) {

        String rootDeviceName = instance.rootDeviceName();
        String volumeId = instance.blockDeviceMappings().stream()
                .filter(mapping -> mapping.deviceName().equals(rootDeviceName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Root device not found"))
                .ebs().volumeId();

        DescribeVolumesRequest volumesRequest = DescribeVolumesRequest.builder()
                .volumeIds(volumeId)
                .build();

        DescribeVolumesResponse volumesResponse = ec2Client.describeVolumes(volumesRequest);
        Volume volume = volumesResponse.volumes().stream()
                .filter(v -> v.volumeId().equals(volumeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Volume not found"));
        logger.info("Root block device has: " + volume.size() + "Gb size.");

        Assert.assertEquals("Root block device size mismatch", Optional.ofNullable(8), Optional.ofNullable(volume.size()));
    }

    private void validateInstanceOS(Instance instance) {

        String instanceOS = instance.platformDetails();
        logger.info("The instance OS is: " + instanceOS);
        Assert.assertEquals("Linux/UNIX", instanceOS);
    }

    private void validatePublicIp(Instance instance) {
        if (instance.iamInstanceProfile().arn().contains("Public")) {
            logger.info("Public instance has public IP: " + instance.publicIpAddress());
            Assert.assertNotNull("Public instance does not have public IP", instance.publicIpAddress());

        } else if (instance.iamInstanceProfile().arn().contains("Private")) {
            logger.info("Private instance has public IP: " + instance.publicIpAddress());
            Assert.assertNotNull("Private instance has public IP", instance.publicIpAddress());
        }
        else {Assert.assertEquals("public or private contains instance", instance.iamInstanceProfile().arn());}
    }
 }
