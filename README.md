# nitron

A multilingual code normalizer application/library.

## How to run

1. Clone this repository

   __*Don't forget '--recursive' option.__
    ```
    git clone --recursive https://github.com/Durun/nitron.git
    ```

1. Prepare JAR file
    - Build manually as `gradlew shadowJar`
    - Or [download JAR file here](https://github.com/Durun/nitron/releases/download/0.1-SNAPSHOT/nitron.jar)

1. Place the config files

   Copy [config directory](https://github.com/Durun/nitron/tree/master/config) into the same directory with `nitron.jar`
   . As below
    ```
    WorkingDir/
    ├ config/
    └ nitron.jar
    ```

1. Run the JAR file

   Run `java -jar nitron.jar [COMMAND] [OPTIONS] [ARGS]`
   **on JVM 11 or higher**

## Command reference

### preparse-register

- *Command:* `preparse-register`
- *Options:*
    - `--lang` Language name defined
      in [config/nitron.json](https://github.com/Durun/nitron/blob/master/config/nitron.json)
    - `--remote` Repository to analyze
- *Args:*
    1. Database file named `cache.db` to save parsetrees
- *Usage:* `preparse-register --lang java --remote https://github.com/githubtraining/hellogitworld cache.db`

for
detail: [RegisterCommand.kt](https://github.com/Durun/nitron/blob/master/src/main/kotlin/io/github/durun/nitron/app/preparse/RegisterCommand.kt)

### preparse-fetch

- *Command:* `preparse-fetch`
- *Options:*
    - `--dir` (optional: default=`tmp`) Working directory to clone the repository
    - `--branch` (optional: default=`master`or`main`) Branch to analyze
    - `--start-date dd:mm:yyyy` `--end-date dd:mm:yyyy` (optional) filter commits by date
- *Args:*
    1. Database file named `cache.db` to save parsetrees
- *Usage:* `preparse-fetch --branch master --start-date 01:01:2012 cache.db`

for
detail: [FetchCommand.kt](https://github.com/Durun/nitron/blob/master/src/main/kotlin/io/github/durun/nitron/app/preparse/FetchCommand.kt)

### preparse

- *Command:* `preparse`
- *Options:*
    - `--repository` Repository URL to analyze
    - `--dir` (optional: default=`tmp`) Working directory to clone the repository
    - `--start-date dd:mm:yyyy` `--end-date dd:mm:yyyy` (optional) filter commits by date
- *Args:*
    1. Database file named `cache.db` to save parsetrees
- *Usage:* `preparse --repository https://github.com/githubtraining/hellogitworld cache.db`

for
detail: [ParseCommand.kt](https://github.com/Durun/nitron/blob/master/src/main/kotlin/io/github/durun/nitron/app/preparse/ParseCommand.kt)