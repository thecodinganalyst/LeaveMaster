package com.practical.leavemaster.leaveeligibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveEligibilityFactControllerTest {

    @Mock LeaveEligibilityFactService service;

    @Test
    void dependantEndpointsMapSuccessAndErrors() {
        LeaveEligibilityFactController controller = new LeaveEligibilityFactController(service);
        StaffDependant dependant = StaffDependant.builder().id("D1").staffId("S1").tenantId("T1").name("Child")
                .relationshipCode("CHILD").active(true).build();
        StaffDependantWriteRequest request = new StaffDependantWriteRequest("Child", "CHILD", null, null, null,
                null, null, null, true);

        when(service.findDependants("S1")).thenReturn(List.of(dependant));
        assertEquals(1, controller.getDependants("S1").getBody().size());
        when(service.findDependants("missing")).thenThrow(new NoSuchElementException());
        assertEquals(404, controller.getDependants("missing").getStatusCode().value());

        when(service.findDependant("S1", "D1")).thenReturn(dependant);
        assertEquals(200, controller.getDependant("S1", "D1").getStatusCode().value());
        when(service.findDependant("S1", "missing")).thenThrow(new NoSuchElementException());
        assertEquals(404, controller.getDependant("S1", "missing").getStatusCode().value());

        when(service.createDependant("S1", request)).thenReturn(dependant);
        assertEquals(201, controller.createDependant("S1", request).getStatusCode().value());
        when(service.createDependant(eq("S1"), any())).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(400, controller.createDependant("S1", request).getStatusCode().value());

        when(service.updateDependant("S1", "D1", request)).thenReturn(dependant);
        assertEquals(200, controller.updateDependant("S1", "D1", request).getStatusCode().value());
        when(service.updateDependant("S1", "missing", request)).thenThrow(new NoSuchElementException());
        assertEquals(404, controller.updateDependant("S1", "missing", request).getStatusCode().value());
        when(service.updateDependant("S1", "bad", request)).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(400, controller.updateDependant("S1", "bad", request).getStatusCode().value());

        assertEquals(204, controller.deleteDependant("S1", "D1").getStatusCode().value());
        doThrow(new NoSuchElementException()).when(service).deleteDependant("S1", "missing");
        assertEquals(404, controller.deleteDependant("S1", "missing").getStatusCode().value());
        doThrow(new IllegalStateException("in use")).when(service).deleteDependant("S1", "used");
        assertEquals(409, controller.deleteDependant("S1", "used").getStatusCode().value());
    }

    @Test
    void qualifyingEventEndpointsMapSuccessAndErrors() {
        LeaveEligibilityFactController controller = new LeaveEligibilityFactController(service);
        QualifyingLeaveEvent event = QualifyingLeaveEvent.builder().id("E1").staffId("S1").tenantId("T1")
                .eventTypeCode("BIRTH").eventDate(LocalDate.of(2026, 1, 1)).status(QualifyingEventStatus.RECORDED).build();
        QualifyingLeaveEventWriteRequest request = new QualifyingLeaveEventWriteRequest(null, "BIRTH",
                LocalDate.of(2026, 1, 1), null, null, null, null, null);

        when(service.findEvents("S1")).thenReturn(List.of(event));
        assertEquals(1, controller.getEvents("S1").getBody().size());
        when(service.findEvents("missing")).thenThrow(new NoSuchElementException());
        assertEquals(404, controller.getEvents("missing").getStatusCode().value());

        when(service.findEvent("S1", "E1")).thenReturn(event);
        assertEquals(200, controller.getEvent("S1", "E1").getStatusCode().value());
        when(service.findEvent("S1", "missing")).thenThrow(new NoSuchElementException());
        assertEquals(404, controller.getEvent("S1", "missing").getStatusCode().value());

        when(service.createEvent("S1", request)).thenReturn(event);
        assertEquals(201, controller.createEvent("S1", request).getStatusCode().value());
        when(service.createEvent(eq("S1"), any())).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(400, controller.createEvent("S1", request).getStatusCode().value());

        when(service.updateEvent("S1", "E1", request)).thenReturn(event);
        assertEquals(200, controller.updateEvent("S1", "E1", request).getStatusCode().value());
        when(service.updateEvent("S1", "missing", request)).thenThrow(new NoSuchElementException());
        assertEquals(404, controller.updateEvent("S1", "missing", request).getStatusCode().value());
        when(service.updateEvent("S1", "bad", request)).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(400, controller.updateEvent("S1", "bad", request).getStatusCode().value());

        ResponseEntity<Void> deleted = controller.deleteEvent("S1", "E1");
        assertEquals(204, deleted.getStatusCode().value());
        doThrow(new NoSuchElementException()).when(service).deleteEvent("S1", "missing");
        assertEquals(404, controller.deleteEvent("S1", "missing").getStatusCode().value());
    }
}
