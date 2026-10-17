package com.codevo.businessos.calendar;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/calendar/events")
public class CalendarEventController {
    private final CalendarEventService service;
    public CalendarEventController(CalendarEventService service) { this.service=service; }
    @GetMapping public PageResponse<CalendarEventDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public CalendarEventDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CalendarEventDtos.View create(@Valid @RequestBody CalendarEventDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public CalendarEventDtos.View update(@PathVariable UUID id,@Valid @RequestBody CalendarEventDtos.Request request) { return service.update(id,request); }
}
