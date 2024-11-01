package aws.aim;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.AttachedPolicy;
import software.amazon.awssdk.services.iam.model.ListAttachedGroupPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListAttachedGroupPoliciesResponse;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class IamGroupTest extends AbstractTest{

    @Test
    public void testFullAccessGroupEC2() {
        validateGroupPolicy("FullAccessGroupEC2", "FullAccessPolicyEC2");
    }

    @Test
    public void testFullAccessGroupS3() {
        validateGroupPolicy("FullAccessGroupS3", "FullAccessPolicyS3");
    }

    @Test
    public void testReadAccessGroupS3() {
        validateGroupPolicy("ReadAccessGroupS3", "ReadAccessPolicyS3");
    }

    private void validateGroupPolicy(String groupName, String expectedPolicyName) {

        ListAttachedGroupPoliciesRequest listAttachedGroupPoliciesRequest = ListAttachedGroupPoliciesRequest.builder()
                .groupName(groupName)
                .build();

        ListAttachedGroupPoliciesResponse listAttachedGroupPoliciesResponse = iamClient.listAttachedGroupPolicies(listAttachedGroupPoliciesRequest);
        List<AttachedPolicy> attachedPolicies = listAttachedGroupPoliciesResponse.attachedPolicies();

        Assert.assertEquals("Number of policies. Actual list is " + attachedPolicies, 1, attachedPolicies.size());

        boolean policyAttached = attachedPolicies.stream()
                .anyMatch(policy -> policy.policyName().equals(expectedPolicyName));
        System.out.println("\n" + attachedPolicies + "\n");

        assertTrue("Policy " + expectedPolicyName + " is not attached to group " + groupName, policyAttached);

        Assert.assertEquals("Policy fo group " + groupName, expectedPolicyName, attachedPolicies.get(0).policyName());
    }
}
