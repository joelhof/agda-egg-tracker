const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

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
         devtool: mode === 'development' ? 'source-map' : 'none'
    };
};