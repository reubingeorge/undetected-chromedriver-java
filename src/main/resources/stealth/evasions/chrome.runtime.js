// chrome.runtime.js - Chrome runtime API mock
(function() {
    'use strict';

    // Ensure chrome object exists
    if (!window.chrome) {
        window.chrome = {};
    }

    // Create chrome.runtime if it doesn't exist
    if (!window.chrome.runtime) {
        window.chrome.runtime = {};
    }

    // Define all runtime enums and constants
    Object.assign(window.chrome.runtime, {
        // Enums
        OnInstalledReason: {
            CHROME_UPDATE: 'chrome_update',
            INSTALL: 'install',
            SHARED_MODULE_UPDATE: 'shared_module_update',
            UPDATE: 'update'
        },

        OnRestartRequiredReason: {
            APP_UPDATE: 'app_update',
            OS_UPDATE: 'os_update',
            PERIODIC: 'periodic'
        },

        PlatformArch: {
            ARM: 'arm',
            ARM64: 'arm64',
            MIPS: 'mips',
            MIPS64: 'mips64',
            X86_32: 'x86-32',
            X86_64: 'x86-64'
        },

        PlatformNaclArch: {
            ARM: 'arm',
            MIPS: 'mips',
            MIPS64: 'mips64',
            X86_32: 'x86-32',
            X86_64: 'x86-64'
        },

        PlatformOs: {
            ANDROID: 'android',
            CROS: 'cros',
            LINUX: 'linux',
            MAC: 'mac',
            OPENBSD: 'openbsd',
            WIN: 'win'
        },

        RequestUpdateCheckStatus: {
            NO_UPDATE: 'no_update',
            THROTTLED: 'throttled',
            UPDATE_AVAILABLE: 'update_available'
        },

        // Properties
        id: undefined,
        lastError: undefined
    });

    // Event mock helper
    const createEvent = () => ({
        addListener: function(callback) {},
        removeListener: function(callback) {},
        hasListener: function(callback) { return false; },
        hasListeners: function() { return false; },
        dispatch: function() {}
    });

    // Port mock helper
    const createPort = () => ({
        name: '',
        disconnect: function() {},
        postMessage: function(message) {},
        onDisconnect: createEvent(),
        onMessage: createEvent()
    });

    // Define methods
    Object.assign(window.chrome.runtime, {
        // Connection methods
        connect: function(extensionId, connectInfo) {
            return createPort();
        },

        connectNative: function(application) {
            return createPort();
        },

        // Messaging methods
        sendMessage: function(extensionId, message, options, callback) {
            if (typeof extensionId === 'function') {
                callback = extensionId;
                extensionId = undefined;
            } else if (typeof message === 'function') {
                callback = message;
                message = extensionId;
                extensionId = undefined;
            } else if (typeof options === 'function') {
                callback = options;
                options = undefined;
            }

            if (callback) {
                setTimeout(() => callback(undefined), 0);
            }
        },

        sendNativeMessage: function(application, message, callback) {
            if (callback) {
                setTimeout(() => callback(undefined), 0);
            }
        },

        // Platform methods
        getPlatformInfo: function(callback) {
            const info = {
                os: 'win', // Default to Windows
                arch: 'x86-64',
                nacl_arch: 'x86-64'
            };

            // Try to detect actual platform
            const ua = navigator.userAgent.toLowerCase();
            if (ua.includes('mac')) {
                info.os = 'mac';
            } else if (ua.includes('linux')) {
                info.os = 'linux';
            } else if (ua.includes('android')) {
                info.os = 'android';
            } else if (ua.includes('cros')) {
                info.os = 'cros';
            }

            if (callback) {
                setTimeout(() => callback(info), 0);
            }
        },

        // Package methods
        getPackageDirectoryEntry: function(callback) {
            if (callback) {
                setTimeout(() => callback(null), 0);
            }
        },

        // Management methods
        getManifest: function() {
            return {
                manifest_version: 2,
                name: 'Chrome',
                version: '1.0.0'
            };
        },

        getURL: function(path) {
            return 'chrome-extension://invalid/' + (path || '');
        },

        // Lifecycle methods
        reload: function() {},

        requestUpdateCheck: function(callback) {
            if (callback) {
                setTimeout(() => callback('no_update', {}), 0);
            }
        },

        restart: function() {},

        restartAfterDelay: function(seconds, callback) {
            if (callback) {
                setTimeout(() => callback(), 0);
            }
        },

        // Extension methods
        getBackgroundPage: function(callback) {
            if (callback) {
                setTimeout(() => callback(null), 0);
            }
        },

        openOptionsPage: function(callback) {
            if (callback) {
                setTimeout(() => callback(), 0);
            }
        },

        setUninstallURL: function(url, callback) {
            if (callback) {
                setTimeout(() => callback(), 0);
            }
        },

        // Events
        onBrowserUpdateAvailable: createEvent(),
        onConnect: createEvent(),
        onConnectExternal: createEvent(),
        onConnectNative: createEvent(),
        onInstalled: createEvent(),
        onMessage: createEvent(),
        onMessageExternal: createEvent(),
        onRestartRequired: createEvent(),
        onStartup: createEvent(),
        onSuspend: createEvent(),
        onSuspendCanceled: createEvent(),
        onUpdateAvailable: createEvent()
    });

    // Make functions look native
    const nativeFunctions = [
        'connect', 'connectNative', 'sendMessage', 'sendNativeMessage',
        'getPlatformInfo', 'getPackageDirectoryEntry', 'getManifest',
        'getURL', 'reload', 'requestUpdateCheck', 'restart',
        'restartAfterDelay', 'getBackgroundPage', 'openOptionsPage',
        'setUninstallURL'
    ];

    nativeFunctions.forEach(funcName => {
        if (window.chrome.runtime[funcName]) {
            window.chrome.runtime[funcName].toString = function() {
                return `function ${funcName}() { [native code] }`;
            };
        }
    });

})();