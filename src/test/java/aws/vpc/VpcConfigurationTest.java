package aws.vpc;

import aws.ec2.AbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.Vpc;

import java.util.stream.Collectors;

public class VpcConfigurationTest extends AbstractTest{
    final static Logger logger = LoggerFactory.getLogger(AbstractTest.class);

    @Test
    public void testVpcConfiguration() {
        Vpc vpc = getNonDefaultVpc();
        validateVpcConfiguration(vpc);
    }

    private Vpc getNonDefaultVpc() {
        DescribeVpcsRequest describeVpcsRequest = DescribeVpcsRequest.builder().build();
        DescribeVpcsResponse describeVpcsResponse = ec2Client.describeVpcs(describeVpcsRequest);

        return describeVpcsResponse.vpcs().stream()
                .filter(vpc -> !vpc.isDefault())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Non-default VPC not found"));
    }

    private void validateVpcConfiguration(Vpc vpc) {
        logger.info("VPC CIDR block: " + vpc.cidrBlock() +"\n");
        Assert.assertEquals("VPC CIDR block mismatch", "10.0.0.0/16", vpc.cidrBlock());
        logger.info("VPC tags contains: " + vpc.tags().stream().filter(tag -> tag.key().equals("cloudx"))
                .collect(Collectors.toList()));
                //.toList());
        ;
    }

    private void validateVpcTag(Vpc vpc) {
        logger.info("VPC CIDR block: " + vpc.tags() +"\n");
        boolean hasCloudxTag =vpc.tags().stream().anyMatch(tag -> "cloudx".equals(tag.key()) && "qa".equals(tag.value()));
        Assert.assertTrue("Instance does not have the correct tags", hasCloudxTag);
        Assert.assertTrue( "VPC tags mismatch", vpc.tags().stream().anyMatch(tag -> tag.key().equals("cloudx") && tag.value().equals("qa")));
    }
 }
