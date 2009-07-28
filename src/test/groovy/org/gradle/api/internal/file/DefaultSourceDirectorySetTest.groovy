/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.file;

import org.apache.tools.ant.Task
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Resource
import org.gradle.util.GFileUtils
import org.gradle.util.HelperUtil
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import org.jmock.integration.junit4.JMock
import org.junit.runner.RunWith
import org.gradle.util.JUnit4GroovyMockery
import org.gradle.api.tasks.StopActionException
import org.gradle.api.file.SourceSet
import org.gradle.api.InvalidUserDataException

@RunWith (JMock)
public class DefaultSourceDirectorySetTest {
    private final JUnit4GroovyMockery context = new JUnit4GroovyMockery()
    private final AntBuilder ant = new AntBuilder()
    private final File testDir = HelperUtil.makeNewTestDir()
    private FileResolver resolver
    private DefaultSourceDirectorySet set

    @Before
    public void setUp() {
        resolver = {src -> new File(testDir, src as String)} as FileResolver
        ant.antProject.addTaskDefinition('test', FileListTask)
        set = new DefaultSourceDirectorySet(resolver)
    }

    @Test
    public void addsResolvedSourceDirectoryToSet() {
        set.srcDir 'dir1'

        assertThat(set.srcDirs, equalTo([new File(testDir, 'dir1')] as Set))
    }

    @Test
    public void addsResolvedSourceDirectoriesToSet() {
        set.srcDir { -> ['dir1', 'dir2'] }

        assertThat(set.srcDirs, equalTo([new File(testDir, 'dir1'), new File(testDir, 'dir2')] as Set))
    }

    @Test
    public void addsFilesetForEachSourceDirectory() {
        File srcDir1 = new File(testDir, 'dir1')
        GFileUtils.touch(new File(srcDir1, 'subdir/file1.txt'))
        GFileUtils.touch(new File(srcDir1, 'subdir/file2.txt'))
        File srcDir2 = new File(testDir, 'dir2')
        GFileUtils.touch(new File(srcDir2, 'subdir2/file1.txt'))

        set.srcDir 'dir1'
        set.srcDir 'dir2'

        assertSetContains(set, 'subdir/file1.txt', 'subdir/file2.txt', 'subdir2/file1.txt')
    }

    @Test
    public void ignoresSourceDirectoriesWhichDoNotExist() {
        File srcDir1 = new File(testDir, 'dir1')
        GFileUtils.touch(new File(srcDir1, 'subdir/file1.txt'))

        set.srcDir 'dir1'
        set.srcDir 'dir2'

        assertSetContains(set, 'subdir/file1.txt')
    }

    @Test
    public void failsWhenSourceDirectoryIsNotADirectory() {
        File srcDir = new File(testDir, 'dir1')
        GFileUtils.touch(srcDir)

        set.srcDir 'dir1'
        try {
            set.addToAntBuilder("node", "fileset")
            fail()
        } catch (InvalidUserDataException e) {
            assertThat(e.message, equalTo("Source directory '$srcDir' is not a directory." as String))
        }
    }

    @Test
    public void throwsStopExceptionWhenNoSourceDirectoriesExist() {
        set.srcDir 'dir1'
        set.srcDir 'dir2'

        try {
            set.stopActionIfEmpty()
            fail()
        } catch (StopActionException e) {
            assertThat(e.message, equalTo('No source files to operate on.'))
        }
    }

    @Test
    public void throwsStopExceptionWhenNoSourceDirectoryHasMatches() {
        set.srcDir 'dir1'
        File srcDir = new File(testDir, 'dir1')
        srcDir.mkdirs()

        try {
            set.stopActionIfEmpty()
            fail()
        } catch (StopActionException e) {
            assertThat(e.message, equalTo('No source files to operate on.'))
        }
    }

    @Test
    public void doesNotThrowStopExceptionWhenSomeSourceDirectoriesAreNotEmpty() {
        set.srcDir 'dir1'
        GFileUtils.touch(new File(testDir, 'dir1/file1.txt'))
        set.srcDir 'dir2'

        set.stopActionIfEmpty()
    }

    @Test
    public void canFilterSourceFiles() {
        File srcDir1 = new File(testDir, 'dir1')
        GFileUtils.touch(new File(srcDir1, 'subdir/file1.txt'))
        GFileUtils.touch(new File(srcDir1, 'subdir/file2.txt'))
        GFileUtils.touch(new File(srcDir1, 'subdir2/file1.txt'))

        set.srcDir 'dir1'

        SourceSet filteredSet = set.matching {
            include '**/file1.txt'
            exclude 'subdir2/**'
        }

        assertSetContains(filteredSet, 'subdir/file1.txt')
    }

    @Test
    public void filteredSetIsLive() {
        File srcDir1 = new File(testDir, 'dir1')
        GFileUtils.touch(new File(srcDir1, 'subdir/file1.txt'))
        GFileUtils.touch(new File(srcDir1, 'subdir/file2.txt'))
        File srcDir2 = new File(testDir, 'dir2')
        GFileUtils.touch(new File(srcDir2, 'subdir2/file1.txt'))

        set.srcDir 'dir1'

        SourceSet filteredSet = set.matching { include '**/file1.txt' }
        assertSetContains(filteredSet, 'subdir/file1.txt')

        set.srcDir 'dir2'

        assertSetContains(filteredSet, 'subdir/file1.txt', 'subdir2/file1.txt')
    }

    private def assertSetContains(SourceSet set, Object ... filenames) {
        FileListTask task = ant.test {
            set.addToAntBuilder(ant, 'thingo')
        }

        assertThat(task.filenames, equalTo(filenames as Set))
    }
}

public static class FileListTask extends Task {
    final Set<String> filenames = new HashSet<String>()

    public void addConfiguredThingo(FileSet fileset) {
        Iterator<Resource> iterator = fileset.iterator()
        while (iterator.hasNext()) {
            Resource resource = iterator.next()
            filenames.add(resource.getName().replace(File.separator, '/'))
        }
    }
}