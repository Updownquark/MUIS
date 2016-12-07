<?xml version="1.0" encoding="UTF-8"?>

<template>
	<body layout="layer">
		<border template-attach-point="border" template-external="false" template-mutable="false">
			<block  layout="border-layout">
				<text-field template-attach-point="text" region="center"
					value="attributes.value" format="${attributes.format}" length="attributes.length" rows="attributes.rows"
					document="${attributes.document}" rich="attributes.rich" multi-line="attributes.multiLine" />
				<block region="right" layout="border-layout">
					<button template-attach-point="up" region="top" action="attributes.increment">
						<image src="up-arrow" width="16" />
					</button>
					<button template-attach-point="down" region="bottom" action="attributes.decrement">
						<image src="down-arrow" width="16" />
					</button>
				</block>
			</block>
		</border>
	</body>
</template>
