const path = require("path");
const HtmlWebPackPlugin = require("html-webpack-plugin");
const ExtractTextPlugin = require("extract-text-webpack-plugin");


module.exports = {
	devtool: 'source-map',
	entry: "./src/index.js",
	output: {
		path: path.join(__dirname, "/dist"),
		filename: "bundle.js",
	},
	devServer: {
		contentBase: path.join(__dirname, 'dist'),
		compress: true,
		historyApiFallback: true,
		port: 3000,
		proxy: {
			'/api/v0/event-discovery-agent': {
				target: 'http://localhost:8120',
				secure: false,
			},
		}
	},

	module: {
		rules: [{
			test: /\.js$/,
			exclude: /node_modules/,
			use: {
				loader: 'babel-loader',
				options: {
					presets: ['@babel/preset-react']
				}
			}
		}, {
			test: /\.css$/,
			use: ExtractTextPlugin.extract(
				{
					fallback: 'style-loader',
					use: ['css-loader']
				}
			)
		},
		{
			test: /\.(png|jpg|gif|svg)$/,
			use: [
				{
					loader: 'file-loader',
					options: {
						name: '[path][name].[ext]',
					},
				}
			]
		},
		{
			test: /\.scss$/,
			use: ['style-loader', 'css-loader', 'sass-loader'],
			issuer: /\.[tj]s$/i
		},
		{
			test: /\.scss$/,
			use: ['css-loader', 'sass-loader'],
			issuer: /\.html?$/i
		},
		{
			test: /\.html$/i, loader: 'html-loader'
		},
		]
	},
	plugins: [
		new HtmlWebPackPlugin({
			hash: true,
			filename: "index.html",  //target html
			template: "./src/index.html" ,//source html
			favicon: './src/assets/img/solace-favicon.png',
		}),
		new ExtractTextPlugin({ filename: 'css/style.css' })
	]
}