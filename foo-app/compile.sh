

mvn clean install

onos-app localhost reinstall org.foo.app /Users/diacio/Desktop/SegmentRouting/foo-app/target/foo-app-1.0-SNAPSHOT.oar

onos localhost app deactivate fwd
onos localhost app activate org.foo.app