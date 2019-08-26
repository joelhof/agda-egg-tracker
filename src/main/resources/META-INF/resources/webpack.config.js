const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const packageJSON = require('./package.json');
var path = require('path');

console.log('Current dir ' +  __dirname);

module.exports = ({ mode }) => {
    return {
        mode,
        entry: {
                app: ['./index.js']
        },
        plugins: [
            new HtmlWebpackPlugin({
                template: './post-current-egg-count.html'
            }),
            new CopyWebpackPlugin([
            {
                context: 'node_modules/@webcomponents/webcomponentsjs',
                from: '**/*.js',
                to: 'webcomponents'
            }
            ])
         ],
         output: {
            path: path.resolve(
                    __dirname, '../../../../../', 'target', 'classes', 'META-INF', 'resources', 'webjars'
                    )
         },
         devtool: mode === 'development' ? 'source-map' : 'none'
    };
};