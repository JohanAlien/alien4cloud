package alien4cloud.deployment;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.orchestrators.services.OrchestratorDeploymentService;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.InputArtifactUtil;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Maps;

@Service
public class DeploymentInputService {

    @Inject
    private ConstraintPropertyService constraintPropertyService;

    @Inject
    private OrchestratorDeploymentService orchestratorDeploymentService;

    /**
     * Fill-in the inputs properties definitions (and default values) based on the properties definitions from the topology.
     *
     * @param topology The deployment topology to impact.
     */
    public void processInputProperties(DeploymentTopology topology) {
        Map<String, String> inputProperties = topology.getInputProperties();
        Map<String, PropertyDefinition> inputDefinitions = topology.getInputs();
        if (inputDefinitions == null || inputDefinitions.isEmpty()) {
            topology.setInputProperties(null);
        } else {
            if (inputProperties == null) {
                inputProperties = Maps.newHashMap();
                topology.setInputProperties(inputProperties);
            } else {
                Iterator<Map.Entry<String, String>> inputPropertyEntryIterator = inputProperties.entrySet().iterator();
                while (inputPropertyEntryIterator.hasNext()) {
                    Map.Entry<String, String> inputPropertyEntry = inputPropertyEntryIterator.next();
                    if (!inputDefinitions.containsKey(inputPropertyEntry.getKey())) {
                        inputPropertyEntryIterator.remove();
                    } else {
                        try {
                            constraintPropertyService.checkSimplePropertyConstraint(inputPropertyEntry.getKey(), inputPropertyEntry.getValue(),
                                    inputDefinitions.get(inputPropertyEntry.getKey()));
                        } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                            // Property is not valid anymore for the input, remove the old value
                            inputPropertyEntryIterator.remove();
                        }
                    }
                }
            }
            for (Map.Entry<String, PropertyDefinition> inputDefinitionEntry : inputDefinitions.entrySet()) {
                String existingValue = inputProperties.get(inputDefinitionEntry.getKey());
                if (existingValue == null) {
                    AbstractPropertyValue defaultValue = inputDefinitionEntry.getValue().getDefault();
                    // we only support scalar properties for topology input properties
                    if (defaultValue != null && defaultValue instanceof ScalarPropertyValue) {
                        inputProperties.put(inputDefinitionEntry.getKey(), ((ScalarPropertyValue) defaultValue).getValue());
                    }
                }
            }
        }
    }

    /**
     * Inject input artifacts in the corresponding nodes.
     */
    public void processInputArtifacts(DeploymentTopology topology) {
        if (topology.getInputArtifacts() != null && !topology.getInputArtifacts().isEmpty()) {
            // we'll build a map inputArtifactId -> List<DeploymentArtifact>
            Map<String, List<DeploymentArtifact>> artifactMap = Maps.newHashMap();
            // iterate over nodes in order to remember all nodes referencing an input artifact
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                if (nodeTemplate.getArtifacts() != null && !nodeTemplate.getArtifacts().isEmpty()) {
                    for (DeploymentArtifact da : nodeTemplate.getArtifacts().values()) {
                        String inputArtifactId = InputArtifactUtil.getInputArtifactId(da);
                        if (inputArtifactId != null) {
                            List<DeploymentArtifact> das = artifactMap.get(inputArtifactId);
                            if (das == null) {
                                das = Lists.newArrayList();
                                artifactMap.put(inputArtifactId, das);
                            }
                            das.add(da);
                        }
                    }
                }
            }

            for (Map.Entry<String, DeploymentArtifact> e : topology.getInputArtifacts().entrySet()) {
                List<DeploymentArtifact> nodeArtifacts = artifactMap.get(e.getKey());
                if (nodeArtifacts != null) {
                    for (DeploymentArtifact nodeArtifact : nodeArtifacts) {
                        nodeArtifact.setArtifactRef(e.getValue().getArtifactRef());
                        nodeArtifact.setArtifactName(e.getValue().getArtifactName());
                        nodeArtifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
                    }
                }
            }
        }
    }

    /**
     * Process default deployment properties
     *
     * @param deploymentTopology the deployment setup to generate configuration for
     */
    public void processProviderDeploymentProperties(DeploymentTopology deploymentTopology) {
        if (deploymentTopology.getOrchestratorId() == null) {
            // No orchestrator assigned for the topology do nothing
            return;
        }
        Map<String, PropertyDefinition> propertyDefinitionMap = orchestratorDeploymentService
                .getDeploymentPropertyDefinitions(deploymentTopology.getOrchestratorId());
        if (propertyDefinitionMap != null) {
            // Reset deployment properties as it might have changed between cloud
            Map<String, String> propertyValueMap = deploymentTopology.getProviderDeploymentProperties();
            if (propertyValueMap == null) {
                propertyValueMap = Maps.newHashMap();
            } else {
                Iterator<Map.Entry<String, String>> propertyValueMapIterator = propertyValueMap.entrySet().iterator();
                while (propertyValueMapIterator.hasNext()) {
                    Map.Entry<String, String> entry = propertyValueMapIterator.next();
                    if (!propertyDefinitionMap.containsKey(entry.getKey())) {
                        // Remove the mapping if topology do not contain the node with that name and of type compute
                        // Or the mapping do not exist anymore in the match result
                        propertyValueMapIterator.remove();
                    }
                }
            }
            for (Map.Entry<String, PropertyDefinition> propertyDefinitionEntry : propertyDefinitionMap.entrySet()) {
                String existingValue = propertyValueMap.get(propertyDefinitionEntry.getKey());
                if (existingValue != null) {
                    try {
                        constraintPropertyService.checkSimplePropertyConstraint(propertyDefinitionEntry.getKey(), existingValue,
                                propertyDefinitionEntry.getValue());
                    } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                        PropertyUtil.setScalarDefaultValueOrNull(propertyValueMap, propertyDefinitionEntry.getKey(), propertyDefinitionEntry.getValue()
                                .getDefault());
                    }
                } else {
                    PropertyUtil.setScalarDefaultValueIfNotNull(propertyValueMap, propertyDefinitionEntry.getKey(), propertyDefinitionEntry.getValue()
                            .getDefault());
                }
            }
            deploymentTopology.setProviderDeploymentProperties(propertyValueMap);
        }
    }

}
