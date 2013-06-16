
//base:=../../../MuisRegistry.xml

[block]{
}

[button]{
	bg={
		transparency=0
		color=blue
		texture="raised-round"
		corner-radius="10%"
	}
	light={
		color=white
		shadow=black
	}
	.hover{
		bg.color=rgb(64, 64, 255)
	}
	.depressed{
		light.color=black
		light.shadow=white
	}
	.!enabled{
		bg.color=lightgray
	}
}
[toggle-button]{
	.selected{
		bg.color=cyan
		.hover{
		}
		.depressed{
		}
	}
}
[label]{
	layout.margin=0;
	layout.padding=0
}

[border]{
	bg.texture=border-texture
	bg.corner-radius=5
	(textfield-border){
		bg.corner-radius=0
		border-style.inset=0
	}
}
