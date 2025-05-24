// navigator.plugins.js - Mock plugins array
(function() {
    'use strict';

    // Check if plugins need to be mocked
    if (navigator.plugins.length > 0) {
        return; // Already has plugins
    }

    // Define mock plugins data
    const mockPlugins = [
        {
            name: 'Chrome PDF Plugin',
            filename: 'internal-pdf-viewer',
            description: 'Portable Document Format',
            mimeTypes: [{
                type: 'application/x-google-chrome-pdf',
                suffixes: 'pdf',
                description: 'Portable Document Format'
            }]
        },
        {
            name: 'Chrome PDF Viewer',
            filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',
            description: 'Portable Document Format',
            mimeTypes: [{
                type: 'application/pdf',
                suffixes: 'pdf',
                description: 'Portable Document Format'
            }]
        },
        {
            name: 'Native Client',
            filename: 'internal-nacl-plugin',
            description: 'Native Client Executable',
            mimeTypes: [
                {
                    type: 'application/x-nacl',
                    suffixes: '',
                    description: 'Native Client Executable'
                },
                {
                    type: 'application/x-pnacl',
                    suffixes: '',
                    description: 'Portable Native Client Executable'
                }
            ]
        }
    ];

    // Create PluginArray mock
    const PluginArray = function() {
        const arr = [];

        arr.item = function(index) {
            return this[index] || null;
        };

        arr.namedItem = function(name) {
            for (let i = 0; i < this.length; i++) {
                if (this[i] && this[i].name === name) {
                    return this[i];
                }
            }
            return null;
        };

        arr.refresh = function() {};

        Object.defineProperty(arr, 'length', {
            get: function() {
                return Object.keys(this).filter(key => !isNaN(parseInt(key))).length;
            },
            enumerable: true,
            configurable: true
        });

        return arr;
    };

    // Create MimeTypeArray mock
    const MimeTypeArray = function() {
        const arr = [];

        arr.item = function(index) {
            return this[index] || null;
        };

        arr.namedItem = function(name) {
            for (let i = 0; i < this.length; i++) {
                if (this[i] && this[i].type === name) {
                    return this[i];
                }
            }
            return null;
        };

        Object.defineProperty(arr, 'length', {
            get: function() {
                return Object.keys(this).filter(key => !isNaN(parseInt(key))).length;
            },
            enumerable: true,
            configurable: true
        });

        return arr;
    };

    // Create Plugin constructor
    const Plugin = function(data) {
        this.name = data.name;
        this.filename = data.filename;
        this.description = data.description;
        this.length = data.mimeTypes.length;

        // Add mime types
        data.mimeTypes.forEach((mimeType, index) => {
            this[index] = {
                type: mimeType.type,
                suffixes: mimeType.suffixes,
                description: mimeType.description,
                enabledPlugin: this
            };
        });

        this.item = function(index) {
            return this[index] || null;
        };

        this.namedItem = function(name) {
            for (let i = 0; i < this.length; i++) {
                if (this[i] && this[i].type === name) {
                    return this[i];
                }
            }
            return null;
        };
    };

    // Create instances
    const pluginArray = new PluginArray();
    const mimeTypeArray = new MimeTypeArray();

    // Populate arrays
    mockPlugins.forEach((pluginData, index) => {
        const plugin = new Plugin(pluginData);
        pluginArray[index] = plugin;

        // Add mime types to global mime type array
        pluginData.mimeTypes.forEach((mimeType, mimeIndex) => {
            const mimeTypeObj = {
                type: mimeType.type,
                suffixes: mimeType.suffixes,
                description: mimeType.description,
                enabledPlugin: plugin
            };

            const globalIndex = Object.keys(mimeTypeArray).filter(key => !isNaN(parseInt(key))).length;
            mimeTypeArray[globalIndex] = mimeTypeObj;
        });
    });

    // Override navigator.plugins
    try {
        Object.defineProperty(navigator, 'plugins', {
            get: function() {
                return pluginArray;
            },
            enumerable: true,
            configurable: true
        });
    } catch (e) {
        // Fallback if property definition fails
        navigator.plugins = pluginArray;
    }

    // Override navigator.mimeTypes
    try {
        Object.defineProperty(navigator, 'mimeTypes', {
            get: function() {
                return mimeTypeArray;
            },
            enumerable: true,
            configurable: true
        });
    } catch (e) {
        // Fallback if property definition fails
        navigator.mimeTypes = mimeTypeArray;
    }

})();