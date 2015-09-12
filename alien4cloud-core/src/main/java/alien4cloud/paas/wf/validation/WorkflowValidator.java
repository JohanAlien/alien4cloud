package alien4cloud.paas.wf.validation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;

@Component
public class WorkflowValidator {

    private List<Rule> rules;

    public WorkflowValidator() {
        super();
        rules = new ArrayList<Rule>();
        rules.add(new CycleDetection());
        rules.add(new StateSequenceValidation());
        rules.add(new NodeValidation());
    }

    public int validate(TopologyContext topologyContext, Workflow workflow) {
        workflow.clearErrors();
        int errorCount = 0;
        for (Rule rule : rules) {
            List<AbstractWorkflowError> errors = rule.validate(topologyContext, workflow);
            if (errors != null) {
                workflow.addErrors(errors);
                errorCount += errors.size();
            }
        }
        return errorCount;
    }

}
