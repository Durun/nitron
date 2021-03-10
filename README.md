# nitron
A multilingual code normalizer application/library.


## How to clone
git clone **--recursive** https://github.com/Durun/nitron.git


## How to build
Build executable into `build/libs/nitron-$version-all.jar`

### Mac, Linux
```shell
./gradlew shadowJar
```

### Windows
```shell
.\gradlew shadowJar
```

## Usage

### Normalize Java code

```shell
java -jar nitron.jar normalize --config config/lang/java.json code.java
```

### Make cache of parsetrees

```shell
preparse-register  cache.db --lang kotlin --remote https://github.com/owner/repository

preparse-fetch cache.db --dir tmp

preparse cache.db --dir tmp --repository https://github.com/owner/repository
# OR > preparse cache.db --dir tmp --all
```
