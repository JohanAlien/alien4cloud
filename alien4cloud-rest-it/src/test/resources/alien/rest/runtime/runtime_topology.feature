Feature: get runtime topology

  Background:
    Given I am authenticated with "ADMIN" role
    And I upload a plugin
    And I create a cloud with name "Mount doom cloud" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-paas-provider"
    And I enable the cloud "Mount doom cloud"
    And I have already created a cloud image with name "Ubuntu Trusty", architecture "x86_64", type "linux", distribution "Ubuntu" and version "14.04.1"
    And I add the cloud image "Ubuntu Trusty" to the cloud "Mount doom cloud" and match it to paaS image "UBUNTU"
    And I add the flavor with name "small", number of CPUs 2, disk size 32 and memory size 2048 to the cloud "Mount doom cloud" and match it to paaS flavor "2"
    And There are these users in the system
      | sangoku |
    And I add a role "APPLICATIONS_MANAGER" to user "sangoku"
    And I add a role "COMPONENTS_MANAGER" to user "sangoku"
    And I add a role "CLOUD_DEPLOYER" to user "sangoku" on the resource type "CLOUD" named "Mount doom cloud"
    And I am authenticated with user named "sangoku"
    And I upload the archive "tosca base types 1.0"
    And I should receive a RestResponse with no error
    And I upload the archive "sample apache lb types 0.1"
    And I should receive a RestResponse with no error
    And I have an application "ALIEN" with a topology containing a nodeTemplate "apacheLBGroovy" related to "fastconnect.nodes.apacheLBGroovy:0.1"
    And I have added a node template "Compute" related to the "tosca.nodes.Compute:1.0" node type
    And I add a relationship of type "tosca.relationships.HostedOn" defined in archive "tosca-base-types" version "1.0" with source "apacheLBGroovy" and target "Compute" for requirement "host" of type "tosca.capabilities.Container" and target capability "compute"

  Scenario: Getting the runtime version of the deployed topology
    Given I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology
    And I have deleted a node template "apacheLBGroovy" from the topology
    When I ask the runtime topology of the application "ALIEN" on the cloud "Mount doom cloud"
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "apacheLBGroovy" and type "fastconnect.nodes.apacheLBGroovy"
    And The RestResponse should contain a nodetemplate named "Compute" and type "tosca.nodes.Compute"
    When I try to retrieve the created topology
    Then I should receive a RestResponse with no error
    And The RestResponse should contain a nodetemplate named "Compute" and type "tosca.nodes.Compute"
    And The RestResponse should not contain a nodetemplate named "apacheLBGroovy"

  Scenario: get_input must be processed in a runtime topology
    Given I define the property "os_arch" of the node "Compute" as input property
    And I set the input property "os_arch" of the topology to "x86_64"
    And I associate the property "os_arch" of a node template "Compute" to the input "os_arch"
    And I deploy the application "ALIEN" with cloud "Mount doom cloud" for the topology
    When I ask the runtime topology of the application "ALIEN" on the cloud "Mount doom cloud"
    Then The topology should contain a nodetemplate named "Compute" with property "os_arch" set to "x86_64"