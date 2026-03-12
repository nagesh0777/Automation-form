package com.automation.podstatus.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS = 120;
  private static final long WINDOW_SECONDS = 60;

  private final Map<String, Counter> counters = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String ip = request.getRemoteAddr();
    Counter counter = counters.computeIfAbsent(ip, key -> new Counter(0, Instant.now().getEpochSecond()));

    synchronized (counter) {
      long now = Instant.now().getEpochSecond();
      if (now - counter.windowStart >= WINDOW_SECONDS) {
        counter.windowStart = now;
        counter.requests = 0;
      }
      counter.requests++;
      if (counter.requests > MAX_REQUESTS) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.getWriter().write("Rate limit exceeded");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private static class Counter {
    int requests;
    long windowStart;

    Counter(int requests, long windowStart) {
      this.requests = requests;
      this.windowStart = windowStart;
    }
  }
}
