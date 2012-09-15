<?xml version="1.0" encoding="UTF-8"?>

<style-sheet xmlns:base="../../../../base/MuisRegistry.xml">
    <head>
        <title>Testing Buttons</title>
        <style-sheet ref="../style/sample.mss" />
    </head>
    <body layout="base:simple">
    	<base:block layout="base:simple" style="bg.color=green" width="100%" top="0" height="100%">
    		<base:block layout="base:simple" left="0" right="100%" top="0" height="25%">This is a MUIS Document</base:block>
			<base:block layout="base:simple" left="0" right="100%" top="25%" height="75%" style="bg.texture=base:raised-round;bg.color=#4040ff;
				bg.corner-radius=15%" style.click="bg.color=purple" style.hover="bg.color=yellow">
				<base:block left="0" right="100%" top="0">The Content</base:block>
			</base:block>
    	</base:block>
    </body>
</muis>
