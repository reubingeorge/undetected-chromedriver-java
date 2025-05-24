// navigator.permissions.js - Permissions API evasion
(function() {
    'use strict';

    // Check if permissions API exists
    if (!navigator.permissions || !navigator.permissions.query) {
        return;
    }

    // Store original query function
    const originalQuery = navigator.permissions.query.bind(navigator.permissions);

    // Permission state mock
    const createPermissionStatus = (state) => {
        const status = {
            state: state,
            onchange: null,
            addEventListener: function() {},
            removeEventListener: function() {},
            dispatchEvent: function() { return true; }
        };

        // Make it look like a real PermissionStatus object
        Object.setPrototypeOf(status, PermissionStatus.prototype);

        return status;
    };

    // Override permissions.query
    navigator.permissions.query = function(permissionDesc) {
        // Handle different permission types
        const name = permissionDesc.name;

        // Common permissions that should appear as 'prompt' state
        const promptPermissions = [
            'notifications',
            'push',
            'midi',
            'camera',
            'microphone',
            'speaker',
            'device-info',
            'background-sync',
            'bluetooth',
            'persistent-storage',
            'ambient-light-sensor',
            'accelerometer',
            'gyroscope',
            'magnetometer',
            'clipboard-read',
            'clipboard-write',
            'payment-handler'
        ];

        // Permissions that should appear as 'granted'
        const grantedPermissions = [
            'geolocation',
            'storage-access'
        ];

        // Check if this is a prompt permission
        if (promptPermissions.includes(name)) {
            return Promise.resolve(createPermissionStatus('prompt'));
        }

        // Check if this is a granted permission
        if (grantedPermissions.includes(name)) {
            return Promise.resolve(createPermissionStatus('granted'));
        }

        // For unknown permissions, use the original function
        try {
            return originalQuery(permissionDesc);
        } catch (e) {
            // If original fails, return prompt state
            return Promise.resolve(createPermissionStatus('prompt'));
        }
    };

    // Make the override less detectable
    navigator.permissions.query.toString = function() {
        return 'function query() { [native code] }';
    };

    // Override Notification.permission if it exists
    if (window.Notification) {
        try {
            Object.defineProperty(Notification, 'permission', {
                get: function() {
                    return 'default';
                },
                enumerable: true,
                configurable: true
            });
        } catch (e) {
            // Some browsers don't allow overriding Notification.permission
        }
    }

    // Handle Notification.requestPermission
    if (window.Notification && window.Notification.requestPermission) {
        const originalRequestPermission = window.Notification.requestPermission;

        window.Notification.requestPermission = function() {
            return new Promise((resolve) => {
                // Simulate user prompt delay
                setTimeout(() => {
                    resolve('default');
                }, 100);
            });
        };

        window.Notification.requestPermission.toString = function() {
            return 'function requestPermission() { [native code] }';
        };
    }

})();