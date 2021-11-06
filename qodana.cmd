docker run --rm -it -p 8080:8080 -v src/:/data/project/ -v build/qodana/:/data/results/ jetbrains/qodana-jvm --show-report
