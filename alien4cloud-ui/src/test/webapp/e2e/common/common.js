/* global browser, protractor, by, element */

'use strict';

var cleanup = require('./cleanup');
var screenshot = require('./screenshot');

// Load languages strings for locale related tests
var frLanguage = require(__dirname + '/../../../../main/webapp/data/languages/locale-fr-fr.json');
var usLanguage = require(__dirname + '/../../../../main/webapp/data/languages/locale-en-us.json');
module.exports.frLanguage = frLanguage;
module.exports.usLanguage = usLanguage;

function log(message) {
  browser.sleep(0).then(function(){ console.log(message); });
}
module.exports.log = log;

function home() {
  browser.get('#/');
  browser.waitForAngular();
}
module.exports.home = home;

module.exports.before = function() {
  // cleanup ElasticSearch and alien folders.
  cleanup.cleanup();
};

// Common utilities to work with protractor
function wElement(selector, fromElement) {
  var selectorStr = selector.toString();
  // wait for the element to be there for 3 sec
  browser.wait(function() {
    var deferred = protractor.promise.defer();
    var isPresentPromise;
    if(fromElement && fromElement !== null) {
      isPresentPromise = fromElement.element(selector).isPresent();
    } else {
      isPresentPromise = browser.element(selector).isPresent();
    }
    isPresentPromise.then(function (isPresent) {
      if(!isPresent) {
        log('waiting for element using selector ' + selectorStr);
      }
      deferred.fulfill(isPresent);
    });
    return deferred.promise;
  }, 3000);
  if(fromElement && fromElement !== null) {
    return fromElement.element(selector);
  }
  return browser.element(selector);
}
module.exports.element = wElement;

function click(selector, fromElement, skipWaitAngular) {
  var target = wElement(selector, fromElement);
  browser.actions().click(target).perform();
  if(!skipWaitAngular) {
    browser.waitForAngular();
  }
  return target;
}
module.exports.click = click;

module.exports.sendKeys = function(selector, keys, fromElement) {
  var target = wElement(selector, fromElement);
  target.sendKeys(keys);
};

module.exports.select = function(selector, value) {
  var selectElement = wElement(selector);
  selectElement.all(by.tagName('option')).then(function() {
    var desiredOption;
    selectElement.all(by.tagName('option'))
      .then(function findMatchingOption(options) {
        options.some(function(option) {
          option.getText().then(function doesOptionMatch(text) {
            if (text.trim() === value) {
              desiredOption = option;
              return true;
            }
          });
        });
      })
      .then(function clickOption() {
        if (desiredOption) {
          desiredOption.click();
        }
      });
  });
};

var confirmAction = function(confirm) {
  // get popover div and grab the Yes / No buttons
  var divPopover = wElement(by.css('.popover'));
  if (confirm === true) { // confirm deletion
    click(by.css('.btn-success'), divPopover);
  } else { // cancel
    click(by.css('.btn-danger'), divPopover);
  }
};
module.exports.confirmAction = confirmAction;

// Remove an element with confirm popover
// The button/a generated by <delete-confirm/> directive has no id
// we have to get the directive child <a> to click on it (or not)
var deleteWithConfirm = function(deleteConfirmDirectiveId, confirm) {
  var deleteConfirm = wElement(by.id(deleteConfirmDirectiveId));
  click(by.tagName('a'), deleteConfirm);
  confirmAction(confirm);
};
module.exports.deleteWithConfirm = deleteWithConfirm;

module.exports.uploadFile = function(path) {
  browser.driver.executeScript('var $scope = angular.element($(\'#fileUpload\')).scope(); $scope.dropSupported=false; $scope.$apply();').then(function() {
    browser.waitForAngular();
    var fileInput = browser.element(by.css('input[type="file"]'));
    fileInput.sendKeys(path);
    screenshot.take('upload');
    browser.waitForAngular();
  });
  browser.waitForAngular();
};
