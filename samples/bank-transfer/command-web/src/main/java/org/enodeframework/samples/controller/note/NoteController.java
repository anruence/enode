package org.enodeframework.samples.controller.note;

import org.enodeframework.commanding.CommandBus;
import org.enodeframework.commanding.CommandReturnType;
import org.enodeframework.common.io.Task;
import org.enodeframework.common.utils.IdGenerator;
import org.enodeframework.samples.commands.note.ChangeNoteTitleCommand;
import org.enodeframework.samples.commands.note.CreateNoteCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@RestController
@RequestMapping("/note")
public class NoteController {

    @Autowired
    private CommandBus commandService;

    @RequestMapping("create")
    public Mono<?> create(@RequestParam("id") String noteId, @RequestParam("t") String title) {
        CreateNoteCommand createNoteCommand = new CreateNoteCommand(noteId, title);
        return Mono.fromFuture(commandService.executeAsync(createNoteCommand, CommandReturnType.EventHandled));
    }

    @RequestMapping("update")
    public Mono<?> update(@RequestParam("id") String noteId, @RequestParam("t") String title) {
        ChangeNoteTitleCommand titleCommand = new ChangeNoteTitleCommand(noteId, title + " change");
        return Mono.fromFuture(commandService.executeAsync(titleCommand, CommandReturnType.EventHandled));
    }

    @RequestMapping("test")
    public Mono<?> test(@RequestParam("id") int totalCount, @RequestParam(required = false, name = "mode", defaultValue = "0") int mode) {
        StopWatch watch = new StopWatch();
        watch.start();
        CountDownLatch latch = new CountDownLatch(totalCount);
        for (int i = 0; i < totalCount; i++) {
            CreateNoteCommand command = new CreateNoteCommand(IdGenerator.id(), "Sample Note" + IdGenerator.id());
            command.setId(String.valueOf(i));
            try {
                CompletableFuture<?> future;
                if (mode == 1) {
                    future = commandService.executeAsync(command, CommandReturnType.EventHandled);
                } else if (mode == 2) {
                    future = commandService.executeAsync(command, CommandReturnType.CommandExecuted);
                } else {
                    future = commandService.sendAsync(command);
                }
                future.whenComplete((result, t) -> {
                    latch.countDown();
                });
            } catch (Exception e) {
                latch.countDown();
            }
        }
        return Mono.fromSupplier(() -> {
            Task.await(latch);
            return watch.getTotalTimeMillis();
        });
    }
}
