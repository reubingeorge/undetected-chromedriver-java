// navigator.webdriver.js - Remove webdriver property
(function() {
    'use strict';

    // Helper function to safely define property
    const safeDefineProperty = (obj, prop, descriptor) => {
        try {
            const existing = Object.getOwnPropertyDescriptor(obj, prop);
            if (!existing || existing.configurable) {
                Object.defineProperty(obj, prop, descriptor);
                return true;
            }
        } catch (e) {
            // Property might be non-configurable
        }
        return false;
    };

    // Primary method: Override the property
    const success = safeDefineProperty(navigator, 'webdriver', {
        get: () => undefined,
        configurable: true,
        enumerable: false
    });

    // Backup method: Delete the property if it exists
    if (!success && navigator.webdriver !== undefined) {
        try {
            delete navigator.webdriver;
        } catch (e) {
            // Some browsers don't allow deletion
        }
    }

    // Additional cleanup: Remove webdriver from window object
    if (window.navigator && window.navigator.webdriver !== undefined) {
        try {
            delete window.navigator.webdriver;
        } catch (e) {}
    }

    // Remove other automation indicators
    const automationProps = [
        '_Selenium_IDE_Recorder',
        '_selenium',
        'calledSelenium',
        '_WEBDRIVER_ELEM_CACHE',
        'ChromeDriverw',
        'driver-evaluate',
        'webdriver-evaluate',
        'selenium-evaluate',
        'webdriverCommand',
        'webdriver-evaluate-response',
        '__webdriverFunc',
        '__driver_evaluate',
        '__webdriver_evaluate',
        '__driver_unwrapped',
        '__webdriver_unwrapped',
        '__webdriver_script_function',
        '__webdriver_script_func',
        '__webdriver_script_fn',
        '__fxdriver_evaluate',
        '__fxdriver_unwrapped',
        '__webdriver_func',
        '__webdriver_scripts',
        '__$webdriverAsyncExecutor',
        '__lastWatirAlert',
        '__lastWatirConfirm',
        '__lastWatirPrompt',
        '$chrome_asyncScriptInfo',
        '$cdc_asdjflasutopfhvcZLmcfl_'
    ];

    // Remove automation properties from window
    automationProps.forEach(prop => {
        try {
            if (window[prop] !== undefined) {
                delete window[prop];
            }
        } catch (e) {}
    });

    // Remove automation properties from document
    automationProps.forEach(prop => {
        try {
            if (document[prop] !== undefined) {
                delete document[prop];
            }
        } catch (e) {}
    });

    // Override Object.getOwnPropertyNames to hide webdriver
    const originalGetOwnPropertyNames = Object.getOwnPropertyNames;
    Object.getOwnPropertyNames = function(obj) {
        const properties = originalGetOwnPropertyNames.apply(this, arguments);
        if (obj === navigator) {
            const index = properties.indexOf('webdriver');
            if (index > -1) {
                properties.splice(index, 1);
            }
        }
        return properties;
    };

    // Override Object.keys to hide webdriver
    const originalKeys = Object.keys;
    Object.keys = function(obj) {
        const keys = originalKeys.apply(this, arguments);
        if (obj === navigator) {
            const index = keys.indexOf('webdriver');
            if (index > -1) {
                keys.splice(index, 1);
            }
        }
        return keys;
    };

})();