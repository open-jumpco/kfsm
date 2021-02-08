@echo off
gradlew -S -i clean build -x dokkaJavadoc -x dokkaGfm -x dokkaJekyll
