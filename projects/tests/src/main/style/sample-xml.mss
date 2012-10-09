<?xml version="1.0" encoding="UTF-8"?>

<style-sheet xmlns:base="../../../../base/MuisRegistry.xml">
	<animate variable="t" init="0">
		<advance to="1" duration="10" />
	</animate>

	bg={
		color=green;
	}
	<category type="base:button">
		bg.color=purple;
		bg.texture="base:raised-round";
		bg.corner-radius="10%";
		<category state="hover">
			bg.color=yellow;
		</category>
		<category state="click">
			bg.color=purple;
		</category>
	</category>
</style-sheet>
