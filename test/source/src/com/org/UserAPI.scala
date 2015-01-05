package com.org

/** apidoc **
  * GROUP: User
  */

class UserAPI {


    /**
     * GET /user/{USER-ID-OR-NAME}
     *
     * Mendapatkan informasi user.
     *
     * + Symbols:
     *
     *      + {USER-ID-OR-NAME} - id user atau nama user.
     *
     * + Parameters:
     *
     *      + ref - reference id.
     *
     * + Tags: public
     */
    def getUserInfo() = {
        // @TODO(you): code here
    }

    /**
     * DELETE /user/{USER-ID-OR-NAME}
     *
     * Untuk menghapus user.
     *
     * + Symbols:
     *
     *      + {USER-ID-OR-NAME} - id user atau nama user.
     *
     * + Tags: private
     */
    def deleteUser() = {
        // @TODO(you): code here
    }

    /**
     * PUT /user/{USER-ID-OR-NAME}
     *
     * Untuk meng-update user
     *
     * + Symbols:
     *
     *      + {USER-ID-OR-NAME} - id user atau nama user.
     *
     * + Parameters:
     *
     *      + full_name=`` - user full name.
     *      + birth_date=`` - user birth date.
     *      + email - user email.
     *
     * + Tags: public
     **/
    def updateUser() = {

    }


}