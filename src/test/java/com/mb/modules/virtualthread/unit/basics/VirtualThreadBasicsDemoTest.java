package com.mb.modules.virtualthread.unit.basics;

import static org.assertj.core.api.Assertions.assertThat;

import com.mb.modules.virtualthread.basics.VirtualThreadBasicsDemo;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VirtualThreadBasicsDemo – API tour output")
class VirtualThreadBasicsDemoTest {

  @Test
  @DisplayName("prints platform vs virtual thread details for every step")
  void printsEveryStep() throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    VirtualThreadBasicsDemo.run(new PrintStream(buffer, true, StandardCharsets.UTF_8));

    String output = buffer.toString(StandardCharsets.UTF_8);
    assertThat(output)
        .contains("[1] Platform thread -> name='demo-platform', isVirtual=false")
        .contains("[2] Virtual thread  -> name='demo-virtual', isVirtual=true")
        .contains("[3] startVirtualThread -> name='', isVirtual=true")
        .contains("[4] Before blocking -> VirtualThread[")
        .contains("[4] After blocking  -> VirtualThread[")
        .contains("[6] main thread -> ");
    assertThat(output.split("\\[5] newVirtualThreadPerTaskExecutor", -1)).hasSize(6);
    assertThat(output)
        .doesNotContain("[5] newVirtualThreadPerTaskExecutor task-0 -> name='', isVirtual=false");
  }
}
