all: debug

debug:
	[ -f local.properties ] || android update project --name HarleyDroid --target android-17 --path .
	ant debug

release:
	ant release

clean:
	rm -rf obj
	ant clean
