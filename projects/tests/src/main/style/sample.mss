
base:=../../../../base/MuisRegistry.xml

>>t=0->1(.004)@10->0(.004)@10

[base:button]
{
	bg={
		texture="base:raised-square"
//		corner-radius="10%"
//		color=blue
		color=rgb(round(t*255), 0, round(255-t*255));
	}
	.hover{
//		bg.color=rgb(round(t*255), round(255-t*255), 0);
	}
	.click{
//		light.source=360*t;
	}
}
