package com.vaadin.demo.nordicsupply.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;

import com.vaadin.demo.nordicsupply.data.ScopeAssignmentRepository;
import com.vaadin.demo.nordicsupply.data.StaffRepository;

/**
 * The signed-in employee and what they may see. The demo has no real login: the rail offers the employees that have
 * a data scope and the choice lives in the Vaadin session. Saved widgets and the activity log belong to this user.
 * The scope comes from the database, not from the screen; in an application with real accounts this one method reads
 * the roles of the authenticated user instead.
 */
@Service
public class CurrentUser {

    private static final String KEY = "aicases.user";

    private static final String FILTER_KEY = "aicases.filter";

    /** The employee as the session keeps it; a value object, not the {@code Staff} entity. */
    public record SignedInUser(int id, String name, String role) {
        @Override
        public String toString() {
            return name + " (" + role + ")";
        }
    }

    private final StaffRepository staff;
    private final ScopeAssignmentRepository assignments;

    public CurrentUser(StaffRepository staff, ScopeAssignmentRepository assignments) {
        this.staff = staff;
        this.assignments = assignments;
    }

    /** The employees that can be signed in: the ones with a data scope, named by the region they cover. */
    public List<SignedInUser> all() {
        var offered = new ArrayList<SignedInUser>();
        for (var assignment : assignments.findAllByOrderByUserId()) {
            var scope = Scopes.roleScope(assignment.getRoleScope());
            var person = staff.findById(assignment.getUserId());
            if (scope.isPresent() && person.isPresent()) {
                var name = person.get().getFullName() == null
                        ? "user " + person.get().getId()
                        : person.get().getFullName();
                offered.add(new SignedInUser(
                        person.get().getId(),
                        Scopes.demoLabel(name, scope.get()),
                        person.get().getRole()));
            }
        }
        return List.copyOf(offered);
    }

    public SignedInUser get() {
        var session = VaadinSession.getCurrent();
        var u = session == null ? null : (SignedInUser) session.getAttribute(KEY);
        if (u == null) {
            var offered = all();
            u = offered.stream()
                    .filter(candidate -> "ADMIN".equals(candidate.role()))
                    .findFirst()
                    .orElseGet(offered::getFirst);
            if (session != null) {
                session.setAttribute(KEY, u);
            }
        }
        return u;
    }

    public void set(SignedInUser user) {
        var session = VaadinSession.getCurrent();
        session.setAttribute(KEY, user);
        session.setAttribute(FILTER_KEY, null);
    }

    public Integer id() {
        return get().id();
    }

    /** The countries the signed-in employee is responsible for. The database enforces this boundary. */
    public Scope scope() {
        int id = get().id();
        return assignments
                .findById(id)
                .flatMap(assignment -> Scopes.roleScope(assignment.getRoleScope()))
                .orElseThrow(() -> new IllegalStateException("employee " + id + " has no data scope"));
    }

    /** What the user narrowed the view to, or the whole scope when they have not narrowed it. */
    public Scope filter() {
        var scope = scope();
        var session = VaadinSession.getCurrent();
        var chosen = session == null ? null : (Scope) session.getAttribute(FILTER_KEY);
        return chosen != null && Scopes.isWithin(chosen, scope) ? chosen : scope;
    }

    /** Narrows the view within the scope; {@code null} clears the choice. */
    public void setFilter(Scope filter) {
        VaadinSession.getCurrent().setAttribute(FILTER_KEY, filter);
    }

    /** The one country the view is narrowed to, or empty when it covers more than one. */
    public Optional<String> filterCountry() {
        var countries = filter().countries();
        return countries.size() == 1 ? Optional.of(countries.getFirst()) : Optional.empty();
    }
}
