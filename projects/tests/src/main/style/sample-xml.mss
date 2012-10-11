<?xml version="1.0" encoding="UTF-8"?>

<style-sheet xmlns:base="../../../../base/MuisRegistry.xml">
	<animate variable="t" init="0">
		<advance to="1" duration="10" />
	</animate>

	bg={
		color=green;
	}
	<category type="base:button">
		bg.color=blue;
		bg.texture="base:raised-square";
		bg.corner-radius="10%";
		<!--light.source=360*t;-->
		<category state="hover">
			bg.color=yellow;
		</category>
		<category state="click">
			bg.color=rgb(t*255, 255-t*255, 0);
		</category>
	</category>
</style-sheet>
