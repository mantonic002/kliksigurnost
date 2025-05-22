# KlikSigurnost - Parental Control System

![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Cloudflare](https://img.shields.io/badge/Cloudflare_Zero_Trust-API-lightgrey)

KlikSigurnost is a student-developed web application that integrates with Cloudflare Zero Trust to help parents manage their children's online presence by creating and controlling access policies for devices. Built with Spring Boot and MySQL, it provides comprehensive features for user management, policy control, and device oversight.

## Features

- **User Authentication**: Secure registration, login, and password management using JWT tokens
- **Zero Trust Policies**: Create, update, and delete Cloudflare Zero Trust policies
- **Device Management**: View and manage devices connected to Cloudflare Zero Trust
- **Support System**: Schedule and manage support appointments
- **Contact Forms**: Submit and manage contact requests (with admin resolution)
- **Notification System**: Track device disconnects and blocked traffic
- **Logs**: View all traffic on connected devices
- **Admin Dashboard**: Comprehensive management interface for administrators

## Technologies

- **Backend**: 
  - Spring Boot 3.1
  - Spring Security
  - Spring Data JPA
- **Database**: MySQL 8.0
- **Integrations**: 
  - Cloudflare Zero Trust API
  - JWT Authentication
- **Tools**:
  - Lombok
  - SLF4J logging
  - Maven

## API Documentation

### Authentication (`/api/auth`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/register` | POST | Register new user |
| `/authenticate` | POST | User login |
| `/refresh` | POST | Refresh JWT tokens |
| `/forgot-password` | POST | Initiate password reset |
| `/reset-password` | POST | Complete password reset |
| `/me` | GET | Get current user profile |
| `/verify` | GET | Verify account email |

### Contact Forms (`/api/contact`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | POST | Submit new contact form |
| `/` | GET | Get all forms (admin) |
| `/pending` | GET | Get pending forms (admin) |
| `/resolve/{id}` | PUT | Resolve form (admin) |

### Appointments (`/api/appointments`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | POST | Schedule new appointment |
| `/` | GET | Get user appointments |
| `/{appointmentId}` | DELETE | Delete appointment |
| `/available` | GET | Get available time slots |

### Zero Trust Policies (`/api/policies`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Get user policies |
| `/` | POST | Create new policy |
| `/{policyId}` | DELETE | Delete policy |
| `/{policyId}` | PUT | Update policy |
| `/devices` | GET | Get user devices |
| `/userLogs` | GET | Get access logs |

### Notifications (`/api/notifications`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Get all notifications |
| `/unseen` | GET | Get unseen notifications |
| `/unseenCount` | GET | Count unseen notifications |
| `/{notificationId}` | DELETE | Delete notification |

### Admin (`/api/admin`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/accounts` | GET | Get all Cloudflare accounts |
| `/accounts/setup` | POST | Create new account |
| `/users` | GET | Get all users |
| `/users/lock/{userId}` | PUT | Toggle user lock |
| `/policies` | GET | Get all policies |
| `/policies/{userId}` | GET | Get policies by user |
| `/appointments` | GET | Get appointments by date range |
| `/contact` | GET | Get all contact forms |
| `/contact/pending` | GET | Get pending forms |
| `/contact/resolve/{id}` | PUT | Resolve contact form |

### Prerequisites
- Java JDK 17+
- MySQL 8.0+
- Maven 3.8+
- Cloudflare account with Zero Trust API access
