const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const packageJSON = require('./package.json');
var path = require('path');

console.log('Current dir ' +  __dirname);

module.exports = ({ mode }) => {
    return {
        mode,
        entry: {
                app: './index.js',
                statistics: './statistics.js'
        },
        plugins: [
            new HtmlWebpackPlugin({
                template: './post-current-egg-count.html'
            }),
             new HtmlWebpackPlugin({
                template: './statistics.html',
                filename: 'statistics.html',
                chunks: ['statistics']
             }),
            new CopyWebpackPlugin([
            {
                context: 'node_modules/@webcomponents/webcomponentsjs',
                from: '**/*.js',
                to: 'webcomponents'
            },
            {
                from: './*.css',
                to: '.'
            }
            ])
         ],
         output: {
            path: path.resolve(
                    __dirname, '../../../', 'target', 'classes', 'META-INF', 'resources'
                    )
         },
        optimization: {
            usedExports: true,
        },
        devtool: mode === 'development' ? 'source-map' : 'none'
    };
};