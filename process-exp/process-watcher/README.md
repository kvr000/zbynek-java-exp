# Process watcher

Utility to run processes and to control their lifecycle, automatically restarting when exited.

## Features

Process tree is described in single JSON file, with the following attributes:
- `command`: the command to run
- `startTimeMs`: time the process initializes, so processing the tree can continue
- `restartDelayMs`: minimal interval to attempt to restart the process
- `disableFile`: file controlling whether the process is disabled
- `dependencies`: list of processes this process depends on

## Usage

```shell
$ process-watcher -h
Usage: process-watcher options... 
ProcessRunner - runs and controls processes

Options:
-s,--spec          definition file of processes
-p,--properties    definition file of tasks
```

## Example

Below is an example of process tree definition:
- `first`: base process
- `second`: base process, with delayed restarts
- `third`: depends on `first` and `second`
- `filedisabled`: process to be run only when `target/disabled` does not exist

```json
{
	"processes": {
		"first": {
			"command": [ "sleep", 20 ],
			"startTimeMs": 1000
		},
		"second": {
			"command": [ "sleep", 8 ],
			"startTimeMs": 2000,
			"restartDelayMs": 10000
		},
		"third": {
			"command": [ "sleep", 30 ],
			"dependencies": [ "first", "second" ]
		},
		"filedisabled": {
			"command": [ "sleep", 300 ],
			"disableFile": "target/disabled"
		},
		"depdisabled": {
			"command": [ "sleep", 200 ],
			"dependencies": [ "filedisabled" ]
		},
		"noterm": {
			"command": [ "perl", "-e", "$SIG{TERM} = sub {}; for (;;) { select(undef, undef, undef, undef); }" ]
		}
	}
}
```

## Build

Maven and Java 11 are required at minimum:
```shell
mvn package
./target/process-watcher -h
```

## About

Homepage: http://github.com/kvr000/zbynek-java-exp/tree/master/process-exp/process-watcher/

Feel free to contact me at kvr000@gmail.com or http://kvr.znj.cz/software/java/ and http://github.com/kvr000

[//]: # ( vim: set tw=120: )
