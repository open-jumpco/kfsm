#!/usr/bin/env sh
./gradlew -i --continue build publishToMavenLocal -x dokkaJavadoc -x dokkaGfm -x dokkaJekyll
