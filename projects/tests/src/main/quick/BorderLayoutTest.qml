<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
	<head>
		<title>Border Layout Test</title>
		<style-sheet ref="../styles/quick-tests.mss"></style-sheet>
	</head>
	<body layout="border-layout" style="bg.color=white">
		<block group="opaque" style="bg.color=red" region="top" height="50" />
		<block group="opaque" style="bg.color=blue" region="right" width="10%" />
		<block group="opaque" style="bg.color=green" region="bottom" height="100" />
		<block group="opaque" style="bg.color=purple" region="left" width="50" />
		<block group="opaque" style="bg.color=pink" region="bottom" height="25%" />
		<block group="opaque" style="bg.color=orange" region="right" width="50" />
		<block group="opaque" style="bg.color=yellow" region="top" height="50" />
		<block group="opaque" style="bg.color=gray" />
	</body>
</quick>
