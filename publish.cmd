@echo off
gradlew -S -i build publishAllPublicationsToMavenRepository publishDocumentationPublicationToMavenRepository -x dokkaJavadoc -x dokkaGfm -x dokkaJekyll
