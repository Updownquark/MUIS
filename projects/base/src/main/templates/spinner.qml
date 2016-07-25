<?xml version="1.0" encoding="UTF-8"?>

<template>
	<head>
		<model name="model" class="org.quick.base.model.SpinnerModel" />
	</head>
	<body layout="layer">
		<border template-attach-point="border" template-external="false" template-mutable="false" layout="border">
			<text-field template-attach-point="text" region="center"
				value="attributes.value" format="attributes.format" length="attributes.length" rows="attributes.rows"
				document="attributes.document" rich="attributes.text" multi-line="attributes.multi-line" />
			<block region="east" layout="border">
				<button template-attach-point="up" region="north" action="attributes.increment" />
				<button template-attach-point="down" region="south" action="attributes.increment" />
			</block>
		</border>
	</body>
</template>
