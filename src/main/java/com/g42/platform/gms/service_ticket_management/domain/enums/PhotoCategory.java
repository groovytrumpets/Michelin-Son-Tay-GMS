package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Enum representing the category of vehicle condition photos.
 * 
 * FRONT, BACK, LEFT, RIGHT: Standard side views (required)
 * TOP, BOTTOM: Additional angle views (optional)
 * OVERALL: General overview photo (optional)
 * ODOMETER: Photo of odometer reading (required)
 * DAMAGE: Photo documenting pre-existing damage (optional, requires description)
 */
public enum PhotoCategory {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    OVERALL,
    ODOMETER,
    DAMAGE
}
