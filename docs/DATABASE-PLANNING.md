# CreatorConnect — Database Planning

## Overview

This document outlines the planned database entities, their attributes, relationships, and module ownership for CreatorConnect. The database will be implemented using **MySQL** with **Spring Data JPA** and **Hibernate** ORM.

For the MVP, a **shared database** approach is used with table naming conventions to indicate module ownership. Post-MVP, this can evolve into a database-per-service architecture.

---

## Entity Relationship Diagram (Conceptual)

```
User (1) ──── (1) CreatorProfile
  │
  └─── (1) ──── (1) FreelancerProfile (1) ──── (0..*) FreelancerSkill (0..*) ──── (1) Skill
                                              │
                                              └─── (0..*) Portfolio
                                              │
                                              └─── (0..*) Review (0..*) ──── (1) Project
                                                                              │
                                                                         CreatorProfile (1)
                                                                              │
                                                                         (0..*) Application (0..*) ──── (1) FreelancerProfile
```

---

## Entity Documentation

### 1. User

**Module Ownership:** Authentication Module

**Purpose:** Core identity record for all platform users. Every person who uses CreatorConnect — whether a Creator or Freelancer — has exactly one User record.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| email | String | Unique, Not Null | User's email address (used for login) |
| password | String | Not Null | BCrypt-hashed password |
| role | Enum | Not Null | CREATOR or FREELANCER |
| createdAt | LocalDateTime | Not Null | Account creation timestamp |
| updatedAt | LocalDateTime | Not Null | Last update timestamp |

**Relationships:**
- One-to-One with `CreatorProfile` (if role = CREATOR)
- One-to-One with `FreelancerProfile` (if role = FREELANCER)

---

### 2. CreatorProfile

**Module Ownership:** Profile Module

**Purpose:** Profile information specific to content creators. Stores the public-facing identity of a creator on the platform.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| userId | Long | FK → User.id, Unique | Reference to User |
| name | String | Not Null | Display name |
| bio | Text | Nullable | Short biography |
| profileImage | String | Nullable | URL to profile image |
| createdAt | LocalDateTime | Not Null | Profile creation timestamp |
| updatedAt | LocalDateTime | Not Null | Last update timestamp |

**Relationships:**
- One-to-One with `User`
- One-to-Many with `Project` (projects posted by this creator)

---

### 3. FreelancerProfile

**Module Ownership:** Profile Module

**Purpose:** Professional profile for freelancers. This is the core entity for talent discovery — it contains all the information that creators search and filter on.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| userId | Long | FK → User.id, Unique | Reference to User |
| name | String | Not Null | Display name |
| bio | Text | Nullable | Professional summary |
| profession | String | Not Null | Primary profession (e.g., "Video Editor") |
| experienceYears | Integer | Nullable | Years of professional experience |
| location | String | Nullable | Geographic location |
| workMode | Enum | Nullable | REMOTE, ONSITE, HYBRID |
| availability | Enum | Nullable | AVAILABLE, UNAVAILABLE, OPEN_TO_OFFERS |
| startingPrice | BigDecimal | Nullable | Minimum project rate |
| profileImage | String | Nullable | URL to profile image |
| rating | Double | Default 0.0 | Average rating from reviews |
| createdAt | LocalDateTime | Not Null | Profile creation timestamp |
| updatedAt | LocalDateTime | Not Null | Last update timestamp |

**Relationships:**
- One-to-One with `User`
- One-to-Many with `FreelancerSkill`
- One-to-Many with `Portfolio`
- One-to-Many with `Review` (reviews received)

---

### 4. Skill

**Module Ownership:** Profile Module

**Purpose:** Skill taxonomy catalog available on the platform. Skills are predefined or dynamically created entities that freelancers can add to their profiles.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| name | String | Unique, Not Null | Skill name (e.g., "Adobe Premiere Pro") |
| category | String | Nullable | Skill category (e.g., "Video Editing") |
| createdAt | LocalDateTime | Not Null | Creation timestamp |

**Relationships:**
- One-to-Many with `FreelancerSkill`

---

### 5. FreelancerSkill

**Module Ownership:** Profile Module

**Purpose:** Junction entity linking a freelancer to a skill with a proficiency level. This enables the many-to-many relationship between freelancers and skills.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| freelancerId | Long | FK → FreelancerProfile.id | Reference to freelancer |
| skillId | Long | FK → Skill.id | Reference to skill |
| proficiencyLevel | Enum | Not Null | BEGINNER, INTERMEDIATE, ADVANCED, EXPERT |

**Relationships:**
- Many-to-One with `FreelancerProfile`
- Many-to-One with `Skill`

**Unique Constraint:** (freelancerId, skillId) — a freelancer cannot have the same skill twice.

---

### 6. Portfolio

**Module Ownership:** Profile Module

**Purpose:** Portfolio item showcasing a freelancer's work. Each item represents a project, piece, or sample the freelancer has created.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| freelancerId | Long | FK → FreelancerProfile.id | Reference to freelancer |
| title | String | Not Null | Portfolio item title |
| description | Text | Nullable | Description of the work |
| mediaUrl | String | Not Null | URL to portfolio media |
| mediaType | Enum | Not Null | IMAGE, VIDEO, LINK, DOCUMENT |
| createdAt | LocalDateTime | Not Null | Creation timestamp |
| updatedAt | LocalDateTime | Not Null | Last update timestamp |

**Relationships:**
- Many-to-One with `FreelancerProfile`

---

### 7. Project

**Module Ownership:** Project Module

**Purpose:** Project listing posted by a creator. This is the central entity around which the hiring workflow revolves.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| creatorId | Long | FK → CreatorProfile.id | Reference to creator who posted |
| title | String | Not Null | Project title |
| description | Text | Not Null | Detailed project description |
| requirements | Text | Nullable | Specific requirements/skills needed |
| budgetMin | BigDecimal | Nullable | Minimum budget |
| budgetMax | BigDecimal | Nullable | Maximum budget |
| timeline | String | Nullable | Expected timeline/duration |
| status | Enum | Not Null, Default: OPEN | OPEN, IN_PROGRESS, COMPLETED, CLOSED |
| createdAt | LocalDateTime | Not Null | Creation timestamp |
| updatedAt | LocalDateTime | Not Null | Last update timestamp |

**Relationships:**
- Many-to-One with `CreatorProfile`
- One-to-Many with `Application`
- One-to-Many with `Review`

---

### 8. Application

**Module Ownership:** Project Module (or Hiring Module)

**Purpose:** Records a freelancer's application to a project. Tracks the status through the hiring workflow.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| projectId | Long | FK → Project.id | Reference to project |
| freelancerId | Long | FK → FreelancerProfile.id | Reference to freelancer |
| coverMessage | Text | Nullable | Application cover message |
| status | Enum | Not Null, Default: PENDING | PENDING, SHORTLISTED, HIRED, REJECTED, WITHDRAWN |
| createdAt | LocalDateTime | Not Null | Application timestamp |
| updatedAt | LocalDateTime | Not Null | Last update timestamp |

**Relationships:**
- Many-to-One with `Project`
- Many-to-One with `FreelancerProfile`

**Unique Constraint:** (projectId, freelancerId) — prevents duplicate applications.

---

### 9. Review

**Module Ownership:** Hiring Module

**Purpose:** Review left by a creator for a freelancer after project completion. Builds freelancer reputation on the platform.

| Attribute | Type | Constraints | Description |
|---|---|---|---|
| id | Long | PK, Auto-increment | Unique identifier |
| projectId | Long | FK → Project.id | Reference to completed project |
| creatorId | Long | FK → CreatorProfile.id | Reference to reviewing creator |
| freelancerId | Long | FK → FreelancerProfile.id | Reference to reviewed freelancer |
| rating | Integer | Not Null, 1–5 | Numerical rating |
| reviewText | Text | Nullable | Written review |
| createdAt | LocalDateTime | Not Null | Review timestamp |

**Relationships:**
- Many-to-One with `Project`
- Many-to-One with `CreatorProfile`
- Many-to-One with `FreelancerProfile`

**Unique Constraint:** (projectId, freelancerId) — prevents duplicate reviews.

---

## Summary of Relationships

| Entity | Relationship | Related Entity | Cardinality |
|---|---|---|---|
| User | → | CreatorProfile | One-to-One (optional) |
| User | → | FreelancerProfile | One-to-One (optional) |
| CreatorProfile | → | Project | One-to-Many |
| FreelancerProfile | → | FreelancerSkill | One-to-Many |
| FreelancerProfile | → | Portfolio | One-to-Many |
| FreelancerProfile | → | Review | One-to-Many |
| Skill | → | FreelancerSkill | One-to-Many |
| Project | → | Application | One-to-Many |
| Project | → | Review | One-to-Many |

---

## Naming Conventions

- **Table names:** Snake case, plural (e.g., `users`, `freelancer_profiles`, `freelancer_skills`)
- **Column names:** Snake case (e.g., `created_at`, `freelancer_id`)
- **JPA entity names:** Pascal case, singular (e.g., `User`, `FreelancerProfile`)
- **Primary keys:** `id` (auto-increment Long)
- **Foreign keys:** `{referenced_entity_snake_case}_id` (e.g., `user_id`, `freelancer_id`)
- **Timestamps:** `created_at`, `updated_at`

---

## Indexing Strategy (Planned)

| Table | Index | Purpose |
|---|---|---|
| users | email (UNIQUE) | Fast login lookup |
| freelancer_profiles | profession | Filter by profession |
| freelancer_profiles | work_mode | Filter by work mode |
| freelancer_profiles | availability | Filter by availability |
| freelancer_profiles | (location, profession) | Combined location + profession search |
| freelancer_skills | (freelancer_id, skill_id) | Unique constraint + fast lookup |
| projects | creator_id | Load creator's projects |
| projects | status | Filter by status |
| projects | (status, created_at) | Browse open projects sorted by date |
| applications | (project_id, freelancer_id) | Unique constraint + fast lookup |
| applications | freelancer_id | Load freelancer's applications |
| reviews | freelancer_id | Load freelancer's reviews |
