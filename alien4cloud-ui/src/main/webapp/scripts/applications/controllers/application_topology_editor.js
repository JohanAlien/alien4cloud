define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('angular-ui-select');

  states.state('applications.detail.topology.editor', {
    url: '/editor',
    templateUrl: 'views/topology/topology_editor.html',
    controller: 'TopologyCtrl',
    resolve: {
      topologyId: function() {
        return null;
      },
      preselectedVersion: function() { return undefined; }
    },
    menu: {
      id: 'am.applications.detail.topology.editor',
      state: 'applications.detail.topology.editor',
      key: 'NAVAPPLICATIONS.MENU_TOPOLOGY',
      icon: 'fa fa-sitemap',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEVOPS'], // is deployer
      priority: 200
    }
  });
  states.forward('applications.detail.topology', 'applications.detail.topology.editor');

  modules.get('a4c-applications', ['ui.select']);
}); // define
