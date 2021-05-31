#!/usr/bin/env sh
./gradlew -i --continue publishLinuxPublicationToMavenRepository -x dokkaJavadoc -x dokkaGfm -x dokkaJekyll
