#!/usr/bin/env sh
./gradlew -i --continue publishToMavenLocal publishLinuxPublicationToMavenRepository -x dokkaJavadoc -x dokkaGfm -x dokkaJekyll
