package com.vaadin.demo.nordicsupply.session;

import java.util.List;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;

import com.vaadin.demo.nordicsupply.data.StaffRepository;

/**
 * The signed-in employee. The demo has no real login: the rail offers the {@code staff} rows and the choice lives in
 * the Vaadin session. Saved widgets and the activity log belong to this user.
 */
@Service
public class CurrentUser {

    private static final String KEY = "aicases.user";

    /** The employee as the session keeps it; a value object, not the {@code Staff} entity. */
    public record SignedInUser(int id, String name, String role) {
        @Override
        public String toString() {
            return name + " (" + role + ")";
        }
    }

    private final StaffRepository staff;

    public CurrentUser(StaffRepository staff) {
        this.staff = staff;
    }

    public List<SignedInUser> all() {
        return staff.findAllByOrderById().stream()
                .map(s -> new SignedInUser(
                        s.getId(), s.getFullName() == null ? "user " + s.getId() : s.getFullName(), s.getRole()))
                .toList();
    }

    public SignedInUser get() {
        var session = VaadinSession.getCurrent();
        var u = session == null ? null : (SignedInUser) session.getAttribute(KEY);
        if (u == null) {
            u = staff.findFirstByRoleOrderById("ADMIN")
                    .map(s -> new SignedInUser(s.getId(), s.getFullName(), s.getRole()))
                    .orElseGet(() -> all().getFirst());
            if (session != null) {
                session.setAttribute(KEY, u);
            }
        }
        return u;
    }

    public void set(SignedInUser user) {
        VaadinSession.getCurrent().setAttribute(KEY, user);
    }

    public Integer id() {
        return get().id();
    }
}
