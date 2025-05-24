// window.chrome.js - Chrome object evasion
(function() {
    'use strict';

    // Check if chrome object already exists
    if (!window.chrome) {
        window.chrome = {};
    }

    // Define chrome.app
    if (!window.chrome.app) {
        window.chrome.app = {
            isInstalled: false,
            InstallState: {
                DISABLED: 'disabled',
                INSTALLED: 'installed',
                NOT_INSTALLED: 'not_installed'
            },
            RunningState: {
                CANNOT_RUN: 'cannot_run',
                READY_TO_RUN: 'ready_to_run',
                RUNNING: 'running'
            },
            getDetails: function() { return null; },
            getIsInstalled: function() { return false; },
            runningState: function() { return 'cannot_run'; }
        };
    }

    // Define chrome.runtime
    if (!window.chrome.runtime) {
        window.chrome.runtime = {
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
            id: undefined,
            connect: function() {
                return {
                    disconnect: function() {},
                    onDisconnect: {
                        addListener: function() {},
                        removeListener: function() {}
                    },
                    onMessage: {
                        addListener: function() {},
                        removeListener: function() {}
                    },
                    postMessage: function() {}
                };
            },
            sendMessage: function() {},
            onMessage: {
                addListener: function() {},
                removeListener: function() {},
                hasListener: function() { return false; }
            },
            onConnect: {
                addListener: function() {},
                removeListener: function() {},
                hasListener: function() { return false; }
            },
            onInstalled: {
                addListener: function() {},
                removeListener: function() {},
                hasListener: function() { return false; }
            },
            onStartup: {
                addListener: function() {},
                removeListener: function() {},
                hasListener: function() { return false; }
            },
            onUpdateAvailable: {
                addListener: function() {},
                removeListener: function() {},
                hasListener: function() { return false; }
            },
            onBrowserUpdateAvailable: {
                addListener: function() {},
                removeListener: function() {},
                hasListener: function() { return false; }
            },
            onRestartRequired: {
                addListener: function() {},
                removeListener: function() {},
                hasListener: function() { return false; }
            },
            getBackgroundPage: function(callback) { callback(null); },
            getManifest: function() { return {}; },
            getURL: function(path) { return ''; },
            reload: function() {},
            requestUpdateCheck: function(callback) {
                if (callback) callback('no_update');
            },
            restart: function() {},
            restartAfterDelay: function() {},
            setUninstallURL: function() {},
            openOptionsPage: function() {},
            getPlatformInfo: function(callback) {
                if (callback) {
                    callback({
                        os: 'win',
                        arch: 'x86-64',
                        nacl_arch: 'x86-64'
                    });
                }
            }
        };
    }

    // Define chrome.csi
    window.chrome.csi = function() {
        return {
            onloadT: Date.now(),
            pageT: Date.now() + Math.random() * 1000,
            startE: Date.now() - Math.random() * 2000,
            tran: Math.random() * 10
        };
    };

    // Define chrome.loadTimes
    window.chrome.loadTimes = function() {
        return {
            commitLoadTime: Date.now() / 1000 - Math.random(),
            connectionInfo: 'h2',
            finishDocumentLoadTime: Date.now() / 1000,
            finishLoadTime: Date.now() / 1000 + Math.random(),
            firstPaintAfterLoadTime: 0,
            firstPaintTime: Date.now() / 1000 - Math.random() * 0.5,
            navigationType: 'Other',
            npnNegotiatedProtocol: 'h2',
            requestTime: Date.now() / 1000 - Math.random() * 2,
            startLoadTime: Date.now() / 1000 - Math.random() * 1.5,
            wasAlternateProtocolAvailable: false,
            wasFetchedViaSpdy: true,
            wasNpnNegotiated: true
        };
    };

    // Additional chrome properties that might be checked
    window.chrome.webstore = {
        install: function() {},
        onInstallStageChanged: {
            addListener: function() {}
        },
        onDownloadProgress: {
            addListener: function() {}
        }
    };

})();