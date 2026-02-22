---
trigger: always_on
---

# MiniChat Project — Agent Rules & Operating System

## PURPOSE

This document defines the permanent operational rules, priorities, constraints, and behavior standards for the AI Agent responsible for designing, developing, and maintaining the MiniChat application.

The Agent MUST follow these rules at all times.

Violation of any rule is considered a critical failure.

---

## CORE PRINCIPLES

1. Production-First Mindset  
   All outputs must be production-ready, scalable, secure, and maintainable.

2. Zero-Assumption Policy  
   Never assume missing requirements.  
   If unclear, document the assumption explicitly.

3. Phase Discipline  
   Never move to the next phase without explicit user approval.

4. Long-Term Maintainability  
   Always prioritize clean code, modularity, and documentation.

5. User Authority  
   The user is the final decision-maker.  
   All instructions override any internal preference.

---

## DEVELOPMENT PHASE CONTROL

### Phase Locking System

The Agent must only work on the active phase.

Current phase is determined exclusively by the user.

Valid commands:

- GO TO NEXT PHASE
- REPEAT CURRENT PHASE
- FREEZE DEVELOPMENT
- RESTART PHASE
- AUDIT SYSTEM

No other input may unlock phases.

---

## QUALITY STANDARDS

### Code Quality

- Follow SOLID strictly.
- No God Classes.
- No Spaghetti Code.
- No Hardcoding.
- No Magic Numbers.
- No Duplicated Logic.
- Full Null Safety.
- 100% Compile-Safe Code.

### Architecture

- Clean Architecture
- MVVM
- Repository Pattern
- Use Case Layer
- Modular Design
- Feature Isolation

### Documentation

Every major component must have:

- Purpose
- Inputs
- Outputs
- Dependencies
- Failure Modes

---

## SECURITY POLICY

### Mandatory

- JWT + Refresh Token
- Token Rotation
- Encrypted Storage
- HTTPS Only
- WebSocket TLS
- Certificate Pinning
- SQL Injection Prevention
- XSS/CSRF Protection
- Brute Force Protection
- Anti-Bot System

### Forbidden

- Plaintext Passwords
- Hardcoded Secrets
- Debug Backdoors
- Insecure Random
- Open Endpoints

---

## PERFORMANCE RULES

- Memory Leak Free
- Low GC Pressure
- Thread-Safe
- Non-Blocking IO
- Async First
- Efficient Serialization
- Connection Pooling

Profiling is mandatory before release.

---

## ERROR MANAGEMENT

### Global Handling

- Centralized Exception Handler
- Unified Error Format
- Retry Strategy
- Graceful Degradation
- User-Friendly Messages

### Logging

- Structured Logs (JSON)
- Correlation IDs
- Severity Levels
- Log Rotation
- No Sensitive Data

---

## COMMUNICATION RULES

### WebSocket

- Heartbeat Required
- Auto-Reconnect
- Session Validation
- Duplicate Connection Prevention
- Flood Protection

### WebRTC

- Secure Signaling
- ICE Fallback
- TURN/STUN Redundancy
- Packet Loss Recovery

---

## DATA MANAGEMENT

### Database

- Normalized Schema
- Indexing Strategy
- Migration System
- Soft Deletes
- Audit Tables

### Caching

- Redis Mandatory
- TTL Policies
- Cache Invalidation
- Consistency Checks

---

## INTERNATIONALIZATION POLICY

- Default: English
- No Hardcoded Strings
- Dynamic Locale
- RTL Support
- Pluralization Rules
- Font Fallback

Server and Client must match.

---

## UI/UX RULES

- Accessibility First
- WCAG Compliance
- Touch Target ≥ 48dp
- Color Contrast AA+
- Animation ≤ 300ms
- No Visual Noise

Dark/Light parity required.

---

## TESTING REQUIREMENTS

### Mandatory

- Unit Tests
- Integration Tests
- Load Tests
- Security Tests
- UI Automation
- Regression Tests

Minimum coverage: 80%

---

## DEPLOYMENT POLICY

- CI/CD Required
- Blue-Green Deployment
- Rollback Support
- Versioned Releases
- Environment Isolation

No manual production deployment.

---

## AI BEHAVIOR RULES

1. Never hallucinate APIs.
2. Never fabricate libraries.
3. Never output incomplete code.
4. Never ignore edge cases.
5. Never skip validation.
6. Never contradict project rules.
7. Always self-audit before output.

---

## CHANGE MANAGEMENT

### Any Change Must Include:

- Impact Analysis
- Migration Plan
- Risk Assessment
- Rollback Plan

No silent changes allowed.

---

## ETHICS & MODERATION

- Content Filtering
- Abuse Detection
- Reporting System
- Ban System
- Privacy Protection
- GDPR Compliance

---

## CRITICAL FAILURE CONDITIONS

Immediate halt if:

- Data leak detected
- Auth bypass possible
- Encryption compromised
- Unauthorized access found
- System instability detected

Must notify user immediately.

---

## VERSION CONTROL RULES

- Git Flow
- Atomic Commits
- Meaningful Messages
- Protected Main Branch
- Mandatory Reviews

---

## FINAL DIRECTIVE

The Agent exists to deliver:

A secure, scalable, high-performance MiniChat platform.

No shortcuts.
No compromises.
No unfinished work.

Always operate at senior architect level.

Failure is not acceptable.
