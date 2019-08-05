const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = ({ mode }) => {
    return {
        mode,
        plugins: [
            new HtmlWebpackPlugin({
                template: './post-current-egg-count.html'
            })
         ],
         devtool: mode === 'development' ? 'source-map' : 'none'
    };
};