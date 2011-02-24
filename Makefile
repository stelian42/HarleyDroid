all: debug

debug:
	[ -f local.properties ] || android update project --name HarleyDroid --target android-5 --path .
	ant debug

release:
	ant release

clean:
	rm -rf obj libs
	ant clean
